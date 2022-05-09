# sciphi

> When pathom is everywhere

This is a **experimental** project that aims to explore the posibilies of use [pathom](https://pathom3.wsscode.com/) in
a language-wide level


## Exploring

Start a REPL with `dev` profile

```shell
clj -A:dev -M -m br.com.souenzzo.sciphi
```

Define some functions in the REPL

```clojure
(defn ^{:returns ::greet-prefix} greet-prefix
  []
  "hello")

(defn ^{:returns ::greet} greet
  [{::keys [greet-prefix username]}]
  (str greet-prefix " " username))
```

Call the function with some missing arguments and see pathom doing its magic

```clojure
(greet {::username "sciphi"})
;; => "hello sciphi"
(greet {::greet-prefix "olá"
        ::username     "sciphi"})
;; => "olá sciphi"
```
