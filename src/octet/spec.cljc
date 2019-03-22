;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;
;; * Redistributions of source code must retain the above copyright notice, this
;;   list of conditions and the following disclaimer.
;;
;; * Redistributions in binary form must reproduce the above copyright notice,
;;   this list of conditions and the following disclaimer in the documentation
;;   and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
;; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
;; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns octet.spec
  "The spec abstraction.

  It includes the basic abstraction protocols for define
  own type specs and some useful types that allows build
  asociative or indexed spec compositions.

  For more examples see the `spec` function docstring."
  (:refer-clojure :exclude [type read float double long short byte bytes repeat])
  (:require [octet.buffer :as buffer]
            [octet.util :refer [assoc-ordered]]))

;; --- Protocols

(defprotocol ISpec
  "Basic abstraction for something that can be work like a Spec."
  (read [_ buff start] "Read all data from buffer.")
  (write [_ buff start data] "Read all data from buffer."))

(defprotocol ISpecWithRef
  "Abstraction to support specs having references to other
   specs within an AssociativeSpec or an IndexedSpec"
  (read* [_ buff start data] "Read data from buffer, use data to calculate length etc")
  (write* [_ buff start value types data] "Write data from buffer, use data to store length etc"))

(defprotocol ISpecSize
  "Abstraction for calculate size of static specs."
  (size [_] "Calculate the size in bytes of the object."))

(defprotocol ISpecDynamicSize
  "Abstraction for calculate size for dynamic specs."
  (size* [_ data] "Calculate the size in bytes of the object having a data."))

;; --- Composed Spec Types

(deftype AssociativeSpec [data dict types]
  #?@(:clj
      [clojure.lang.Counted
       (count [_] (count types))]
      :cljs
      [cljs.core/ICounted
       (-count [_] (count types))])

  ISpecSize
  (size [_]
    (reduce #(+ %1 (size %2)) 0 types))

  ISpecDynamicSize
  (size* [_ data]
    (reduce (fn [acc [field data]]
              (let [type (field dict)]
                (if (satisfies? ISpecDynamicSize type)
                  (+ acc (size* type data))
                  (+ acc (size type)))))
            0
            (into [] data)))

  ISpec
  (read [_ buff pos]
    (loop [index pos result (array-map) pairs data]
      (if-let [[fieldname type] (first pairs)]
        (let [[readedbytes readeddata]
              (if (satisfies? ISpecWithRef type)
                (read* type buff index result)
                (read type buff index))]
          (recur (+ index readedbytes)
                 (assoc-ordered result fieldname readeddata)
                 (rest pairs)))
          [(- index pos) result])))

  (write [_ buff pos data']
    (let [written
          (reduce (fn [index [fieldname type]]
                    (let [value (get data' fieldname nil)
                          written (if (satisfies? ISpecWithRef type)
                                    (write* type buff index value dict data')
                                    (write type buff index value))]
                      (+ index written)))
                  pos data)]
      (- written pos))))

(deftype IndexedSpec [types]
  #?@(:clj
      [clojure.lang.Counted
       (count [_] (count types))]
      :cljs
      [cljs.core/ICounted
       (-count [_] (count types))])

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
        (let [[readedbytes readeddata]
              (if (satisfies? ISpecWithRef type)
                (read* type buff index result)
                (read type buff index))]
          (recur (+ index readedbytes)
                 (conj result readeddata)
                 (rest types)))
        [(- index pos) result])))

  (write [_ buff pos data']
    (let [indexedtypes (map-indexed vector types)
          written (reduce (fn [pos [index type]]
                            (let [value (nth data' index nil)
                                  written (if (satisfies? ISpecWithRef type)
                                            (write* type buff pos value types data')
                                            (write type buff pos value))]
                              (+ pos written)))
                          pos indexedtypes)]
      (- written pos))))

#?(:clj
   (do
     (alter-meta! #'->AssociativeSpec assoc :private true)
     (alter-meta! #'->IndexedSpec assoc :private true)))

;; --- Spec Constructors

(defn- spec? [s]
  (or (satisfies? ISpecWithRef s)
      (satisfies? ISpec s)))

(defn- associative-spec
  [& params]
  (let [data (mapv vec (partition 2 params))
        dict (apply array-map params)
        types (map second data)]
    (AssociativeSpec. data dict types)))

(defn- indexed-spec
  [& types]
  (IndexedSpec. types))

(defn spec
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
  [& params]
  (let [numparams (count params)]
    (cond
      (every? spec? params)
      (apply indexed-spec params)

      (and (even? numparams)
           (keyword? (first params))
           (spec? (second params)))
      (apply associative-spec params)

      :else
      (throw (ex-info "Invalid arguments" {})))))

(defn repeat
  "Creare a composed typespec that repeats `n` times
  a provided `type` spec.

  As example, create a spec with help of `repeat`
  function:

      (def spec (buf/repeat 2 buf/int32))

  Write data into buffer using previously defined
  typespec:

      (buf/write! yourbuffer [200 300] spec)
      ;; => 8

  Or read data from your buffer using previously defined
  typespec:

      (buf/read yourbuffer spec)
      ;; => [200 300]
  "
  [n type]
  (reify
    ISpecSize
    (size [_] (* n (size type)))

    ISpecDynamicSize
    (size* [_ data]
      (reduce (fn [acc [index data]]
                (if (satisfies? ISpecSize type)
                  (+ acc (size type))
                  (+ acc (size* type data))))
              0
              (map-indexed vector data)))

    ISpec
    (read [_ buff pos]
      (loop [index pos result [] types (take n (repeatedly (constantly type)))]
        (if-let [type (first types)]
          (let [[readedbytes readeddata] (read type buff index)]
            (recur (+ index readedbytes)
                   (conj result readeddata)
                   (rest types)))
          [(- index pos) result])))

    (write [_ buff pos data]
      (loop [data data written pos]
        (if-let [value (first data)]
          (let [written' (write type buff written value)]
            (recur (rest data)
                   (+ written written')))
          (- written pos))))))

(defn compose
  "Create a composed typespec with user defined
  type constructor.

  This allows define a typespec with automatic conversion
  between user data type and serialized data without
  defining yourself a datatype using low level constructions.

  Let see an exameple for understand it better.

  Imagine you are have this data type:

      (defrecord Point [x y])

  With help of `compose`, create a new spec
  for your datatype:

      (def point-spec (buf/compose ->Point [buf/int32 buf/int32]))

  Now, you can use the previously defined datatype and typespec
  for write into buffer:

      (buf/write! buffer mypoint point-spec)
      ;; => 8

  Or read from the buffer:

      (buf/read buffer (point))
      ;; => #user.Point{:x 1, :y 2}
  "
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
