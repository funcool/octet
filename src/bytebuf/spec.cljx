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
  clojure.lang.Counted
  (count [_]
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
  clojure.lang.Counted
  (count [_]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bool
  "Boolean type spec constructor."
  ([] (bool nil))
  ([default]
   (reify
     IStaticSize
     (size [_] 1)

     ISpec
     (read [_ buff pos]
       (let [readed (buffer/read-byte buff pos)]
         [1 (condp = readed
              (clojure.core/byte 0) false
              (clojure.core/byte 1) true
              nil)]))

     (write [_ buff pos value]
       (let [value (condp = value
                     true (clojure.core/byte 1)
                     false (clojure.core/byte 0)
                     (clojure.core/byte -1))]
         (buffer/write-byte buff pos value)
         1)))))

(defn byte
  "Byte type spec constructor."
  ([] (byte (clojure.core/byte 0)))
  ([default]
   (reify
     IStaticSize
     (size [_] 1)

     ISpec
     (read [_ buff pos]
       (let [readed (buffer/read-byte buff pos)]
         [1 readed]))

     (write [_ buff pos value]
       (let [value (clojure.core/byte (or value default))]
         (buffer/write-byte buff pos value)
         1)))))

(defn int16
  "Short type spec constructor."
  ([] (int16 0))
  ([default]
   (reify
     IStaticSize
     (size [_] 2)

     ISpec
     (read [s buff pos]
       [(proto/size s)
        (buffer/read-short buff pos)])

     (write [s buff pos value]
       (let [value (or value default)]
         (buffer/write-short buff pos value)
         (proto/size s))))))

(defn int32
  "Integer type spec constructor."
  ([] (int32 0))
  ([default]
   (reify
     IStaticSize
     (size [_] 4)

     ISpec
     (read [s buff pos]
       [(proto/size s)
        (buffer/read-int buff pos)])

     (write [s buff pos value]
       (let [value (or value default)]
         (buffer/write-int buff pos value)
         (proto/size s))))))

(defn int64
  "Long type spec constructor."
  ([] (int64 0))
  ([default]
   (reify
     IStaticSize
     (size [_] 8)

     ISpec
     (read [s buff pos]
       [(proto/size s)
        (buffer/read-long buff pos)])

     (write [s buff pos value]
       (let [value (or value default)]
         (buffer/write-long buff pos value)
         (proto/size s))))))

(defn float
  "Float type spec constructor."
  ([] (float 0))
  ([default]
   (reify
     IStaticSize
     (size [_] 4)

     ISpec
     (read [s buff pos]
       [(proto/size s)
        (buffer/read-float buff pos)])

     (write [s buff pos value]
       (let [value (or value default)]
         (buffer/write-float buff pos value)
         (proto/size s))))))

(defn double
  "Double type spec constructor."
  ([] (double 0))
  ([default]
   (reify
     IStaticSize
     (size [_] 8)

     ISpec
     (read [s buff pos]
       [(proto/size s)
        (buffer/read-double buff pos)])

     (write [s buff pos value]
       (let [value (or value default)]
         (buffer/write-double buff pos value)
         (proto/size s))))))

(defn bytes
  "Fixed size byte array type spec constructor."
  [^long size]
  (reify
    IStaticSize
    (size [_] size)

    ISpec
    (read [_ buff pos]
      [size (buffer/read-bytes buff pos size)])

    (write [_ buff pos value]
      (buffer/write-bytes buff pos size value)
      size)))

(defn string
  "Fixed size string type spec constructor."
  [^long size]
  (reify
    IStaticSize
    (size [_] size)

    ISpec
    (read [_ buff pos]
      (let [rawdata (buffer/read-bytes buff pos size)
            length  (- size (bytes/zeropad-count rawdata))
            data (String. rawdata 0 length "UTF-8")]
        [size data]))

    (write [_ buff pos value]
      (let [input (.getBytes value "UTF-8")
            length (count input)
            tmpbuf (byte-array size)]
        (if (< length size)
          (System/arraycopy input 0 tmpbuf 0 length)
          (System/arraycopy input 0 tmpbuf 0 size))

        (when (< length size)
          (bytes/zeropad! tmpbuf length))

        (buffer/write-bytes buff pos size tmpbuf)
        size))))

(defn string*
  "Arbitrary length string type spec constructor."
  []
  (reify
    ISpec
    (read [_ buff pos]
      (let [datasize (buffer/read-int buff pos)
            data (buffer/read-bytes buff (+ pos 4) datasize)
            data (String. data 0 datasize "UTF-8")]
        [(+ datasize 4) data]))

    (write [_ buff pos value]
      (let [input (.getBytes value "UTF-8")
            length (count input)]
        (buffer/write-int buff pos length)
        (buffer/write-bytes buff (+ pos 4) length input)
        (+ length 4)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types Alias
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Alias for int32"}
  integer int32)

(def ^{:doc "Alias for int64"}
  long int64)

(def ^{:doc "Alias for int16"}
  short int16)
