(ns octet.util)

#+clj
(defmacro defalias
  [sym sym2]
  `(do
     (def ~sym ~sym2)
     (alter-meta! (var ~sym) merge (dissoc (meta (var ~sym2)) :name))))


