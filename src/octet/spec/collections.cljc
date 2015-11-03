(ns octet.spec.collections
  (:require [octet.buffer :as buffer]
            [octet.spec :as spec]))

(defn vector*
  "Create a arbitrary length (dynamic) array like
  typespec composition for `t` type."
  [t]
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
      (+ 4 (reduce #(+ %1 (if (satisfies? spec/ISpecDynamicSize t)
                            (spec/size* t %2)
                            (spec/size t))) 0 data)))
    spec/ISpec
    (read [_ buff pos]
      (let [nitems (buffer/read-int buff pos)
            typespec (spec/repeat nitems t)
            [readed data] (spec/read typespec buff (+ pos 4))]
        [(+ readed 4) data]))

    (write [_ buff pos input]
      (let [nitems (count input)
            typespec (spec/repeat nitems t)]
        (buffer/write-int buff pos nitems)
        (+ 4 (spec/write typespec buff (+ pos 4) input))))))
