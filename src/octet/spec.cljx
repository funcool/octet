(ns octet.spec
  "The spec abstraction.

  It includes the basic abstraction protocols for define
  own type specs and some useful types that allows build
  asociative or indexed spec compositions.

  For more examples see the `spec` function docstring."
  (:refer-clojure :exclude [type read float double long short byte bytes])
  (:require [octet.buffer :as buffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ISpec
  "Basic abstraction for something that can be work like a Spec."
  (read [_ buff start] "Read all data from buffer.")
  (write [_ buff start data] "Read all data from buffer."))

(defprotocol ISpecSize
  "Abstraction for calculate size of static specs."
  (size [_] "Calculate the size in bytes of the object."))

(defprotocol ISpecDynamicSize
  "Abstraction for calculate size for dynamic specs."
  (size* [_ data] "Calculate the size in bytes of the object having a data."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Composed Spec Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype AssociativeSpec [data dict types]
  #+clj
  clojure.lang.Counted
  #+clj
  (count [_]
    (count types))

  #+cljs
  ICounted
  #+cljs
  (-count [_]
    (count types))

  ISpecSize
  (size [_]
    (reduce #(+ %1 (size %2)) 0 types))

  ISpecDynamicSize
  (size* [_ data]
    (reduce (fn [acc [field data]]
              (let [type (field dict)]
                (if (satisfies? ISpecSize type)
                  (+ acc (size type))
                  (+ acc (size* type data)))))
            0
            (into [] data)))

  ISpec
  (read [_ buff pos]
    (loop [index pos result {} pairs data]
      (if-let [[fieldname type] (first pairs)]
        (let [[readedbytes readeddata] (read type buff index)]
          (recur (+ index readedbytes)
                 (assoc result fieldname readeddata)
                 (rest pairs)))
        [(- index pos) result])))

  (write [_ buff pos data']
    (let [written (reduce (fn [index [fieldname type]]
                            (let [value (get data' fieldname nil)
                                  written (write type buff index value)]
                              (+ index written)))
                          pos data)]
      (- written pos))))

(deftype IndexedSpec [types]
  #+clj
  clojure.lang.Counted
  #+clj
  (count [_]
    (count types))

  #+cljs
  ICounted
  #+cljs
  (-count [_]
    (count types))

  ISpecSize
  (size [_]
    (reduce #(+ %1 (size %2)) 0 types))

  ISpecDynamicSize
  (size* [_ data]
    (reduce (fn [acc [index data]]
              (let [type (nth types index)]
                (if (satisfies? ISpecSize type)
                  (+ acc (size type))
                  (+ acc (size* type data)))))
            0
            (map-indexed vector data)))

  ISpec
  (read [_ buff pos]
    (loop [index pos result [] types types]
      (if-let [type (first types)]
        (let [[readedbytes readeddata] (read type buff index)]
          (recur (+ index readedbytes)
                 (conj result readeddata)
                 (rest types)))
        [(- index pos) result])))

  (write [_ buff pos data']
    (let [indexedtypes (map-indexed vector types)
          written (reduce (fn [pos [index type]]
                            (let [value (nth data' index nil)
                                  written (write type buff pos value)]
                              (+ pos written)))
                          pos indexedtypes)]
      (- written pos))))

#+clj (alter-meta! #'->AssociativeSpec assoc :private true)
#+clj (alter-meta! #'->IndexedSpec assoc :private true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti spec
  "Polymorphic constructor for Spec instances.

  Spec is a some kind of composition of arbitrary
  number of types in associative or indexed data
  structure.

  Little example on how to create associative
  composition:

      (spec :field1 (long)
            :field2 (string 20))

  An other example on how to create indexed
  composition that represents the same bytes
  representation that previous one:

      (spec (long) (string 20))

  The main difference between the two reprensentation
  is that if you read a buffer using an associative
  spec, the result will be clojure hash-map, and if
  indexed spec is used, the result will be clojure
  vector containing the values.

  The same rules applies for writing data into a
  buffer."
  (fn [& params]
    (let [numparams (count params)]
      (cond
        (every? #(satisfies? ISpec %) params)
        :indexed

        (and (even? numparams)
             (keyword? (first params))
             (satisfies? ISpec (second params)))
        :associative))))

(defmethod spec :associative
  [& params]
  (let [data (mapv vec (partition 2 params))
        dict (into {} data)
        types (map second data)]
    (AssociativeSpec. data dict types)))

(defmethod spec :indexed
  [& types]
  (IndexedSpec. types))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec Composition Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn compose
  "Constructor of composed typespecs with specific constructor.

  This is very usefull when you have some datatype and
  you want serialize and deserialize it on your binary
  protocol.

  As example code, imagine you are have this data type:

      ;; Imagine that your have this datatype.
      (defrecord Point [x y])

  With help of `compose` function, create a new spec
  for your datatype:

      (def point-spec (buf/compose ->Point [buf/int32 buf/int32]))

  Now, you can use the previously defined datatype for write
  into buffer:

      (buf/write! buffer mypoint point-spec)
      ;; => 8

  Or read from the buffer:

      (buf/read buffer (point))
      ;; => #user.Point{:x 1, :y 2}

  This a helper for avoid learn the internals of octer library
  for build a specific spec constructor for your data type."
  [constructor types]
  {:pre [(fn? constructor)
         (vector? types)]}
  (let [spec' (apply spec types)]
    (reify
      ISpecSize
      (size [_] (size spec'))

      ISpec
      (read [_ buff pos]
        (let [[readed data] (read spec' buff pos)]
          [readed (apply constructor data)]))

      (write [_ buff pos data']
        (let [data' (vec (vals data'))]
          (write spec' buff pos data'))))))
