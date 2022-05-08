(ns br.com.souenzzo.sciphi
  (:require [sci.core :as sci]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [clojure.core.specs.alpha]
            [babashka.nrepl.server :as bbns]
            [com.wsscode.pathom3.connect.operation :as pco]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]))

(defn custom-defn
  [form env & args]
  (let [{:keys [fn-name fn-tail]}
        (s/conform :clojure.core.specs.alpha/defn-args
          args)
        {:keys [returns]} (meta fn-name)
        output [returns]
        argcount (-> fn-tail second :params :params count)]
    (if returns
      (do
        (assert (= :arity-1 (first fn-tail))
          "You can't use multiple signatures with returns")
        (assert (<= 0 argcount 2)
          "Functions that returns can only have 0, 1, or 2 args.")
        (let [[_ v] (-> (s/conform
                          ::pco/defresolver-args
                          (list (gensym) (second args) {}))
                      pco/normalize-arglist
                      :arglist
                      last)
              input (gensym "input")
              env (gensym "env")
              form `(do
                      (~'reg!
                       (pco/resolver (symbol ~(str (symbol returns)))
                         ~{::pco/input  (pco/extract-destructure-map-keys-as-keywords v)
                           ::pco/output output}
                         (fn [~env ~input]
                           {~returns ~(case argcount
                                        0 (list (cons 'fn (rest form)))
                                        1 (list (cons 'fn (rest form)) input)
                                        2 (list (cons 'fn (rest form)) env input))})))
                      (defn ~fn-name
                        ([]
                         (~'process! ~returns))
                        ([input#]
                         (~'process! input# ~returns))
                        ([env# input#]
                         (~'process! env# input# ~returns))))]
          form))
      (cons 'clojure.core/defn (rest form)))))


(defn init
  [opts]
  (let [*idx (atom {})]
    (-> opts
      (update :namespaces merge {'com.wsscode.pathom3.interface.eql     (ns-publics 'com.wsscode.pathom3.interface.eql)
                                 'com.wsscode.pathom3.connect.indexes   (ns-publics 'com.wsscode.pathom3.connect.indexes)
                                 'com.wsscode.pathom3.connect.operation (ns-publics 'com.wsscode.pathom3.connect.operation)})
      (update :bindings merge {'process! (fn
                                           ([returns]
                                            (-> @*idx
                                              (p.eql/process [returns])
                                              (get returns)))
                                           ([input returns]
                                            (-> @*idx
                                              (p.eql/process input [returns])
                                              (get returns)))
                                           ([env input returns]
                                            (-> (merge @*idx env)
                                              (p.eql/process input [returns])
                                              (get returns))))
                               'reg!     (fn [op]
                                           (swap! *idx (fn [idx]
                                                         (let [op-name (::pco/op-name (pco/operation-config op))]
                                                           (-> idx
                                                             (update ::pci/index-resolvers dissoc op-name)
                                                             (pci/register op))))))
                               'defn     ^{:macro true}
                                         (fn [& args]
                                           (apply custom-defn args))})
      (sci/init))))

(defn hello
  []
  (let [ctx (init {})]
    (->> ["(do"
          "(defn ^{:returns ::foo} foo [] 42)"
          "(defn ^{:returns ::bar} bar [{::keys [foo baz]}] (+ foo baz))"
          "(bar {::baz 1})"
          ")"]
      (string/join "\n")
      (sci/eval-string* ctx))))

(comment
  (hello))

(defn eval-string
  ([s]
   (sci/eval-string* (init {}) s))
  ([s opts]
   (sci/eval-string* (init opts) s)))

(defn nrepl
  [& args]
  (let [ctx (init {:namespaces {'cursive.repl.runtime       (ns-publics 'cursive.repl.runtime)
                                'cursive.repl.runtime.class (ns-publics 'cursive.repl.runtime.class)}})]
    (bbns/start-server! ctx {:host "127.0.0.1"
                             :port 23456})))

(defn -main
  [& _]
  (let [ctx (init {})
        repl (requiring-resolve 'clojure.main/repl)]
    (repl :read (fn [request-prompt request-exit]
                  (sci/parse-next ctx *in*))
      :eval (fn [form]
              (sci/eval-form ctx form)))))
