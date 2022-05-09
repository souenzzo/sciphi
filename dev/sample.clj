#_(ns sample)


(defn ^{:returns ::greet-prefix} greet-prefix
  []
  "hello")

(defn ^{:returns ::greet} greet
  [{::keys [greet-prefix username]}]
  (str greet-prefix " " username))

(comment
  (greet {::username "sciphi"})
  (greet {::greet-prefix "olá"
          ::username     "sciphi"}))

