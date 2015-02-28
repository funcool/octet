(ns bytebuf.spec
  (:refer-clojure :exclude [type read float double long short byte bytes])
  (:require [bytebuf.proto :as proto :refer [IStaticSize]]
            [bytebuf.buffer :as buffer]
            [bytebuf.bytes :as bytes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ISpec
  "Basic abstraction for something that can be work like a Spec."
  (read [_ buff start] "Read all data from buffer.")
  (write [_ buff start data] "Read all data from buffer."))

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

  IStaticSize
  (size [_]
    (reduce #(+ %1 (proto/size %2)) 0 types))

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

  IStaticSize
  (size [_]
    (reduce #(+ %1 (proto/size %2)) 0 types))

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

(def ^{:doc "A semantic alias for spec constructor."}
  compose-type spec)
