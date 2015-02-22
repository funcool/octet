(ns bytebuf.spec
  (:refer-clojure :exclude [type read])
  (:require [bytebuf.proto :as proto :refer [IStaticSize]]
            [bytebuf.buffer :as buffer]
            [bytebuf.bytes :as bytes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ISpecType
  (tag [_] "Get the type tag."))

(defprotocol IReadableSpec
  (read [_ buff start] "Read all data from buffer."))

(defprotocol IWritableSpec
  (write [_ buff start data] "Read all data from buffer."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti spec
  (fn [& params]
    (let [numparams (count params)]
      (cond
        (and (even? numparams)
             (keyword? (first params))
             (satisfies? ISpecType (second params)))
        :associative

        (every? #(satisfies? ISpecType %) params)
        :indexed))))

(defmethod spec :associative
  [& params]
  (let [data (mapv vec (partition 2 params))
        dict (into {} data)
        types (map second data)]
    (reify
      clojure.lang.Counted
      (count [_]
        (count types))

      ISpecType
      (tag [_] :static)

      IStaticSize
      (size [_]
        (reduce #(+ %1 (proto/size %2)) 0 types))

      IReadableSpec
      (read [_ buff pos]
        (loop [index pos result {} pairs data]
          (if-let [[fieldname type] (first pairs)]
            (let [[readedbytes readeddata] (read type buff index)]
              (recur (+ index readedbytes)
                     (assoc result fieldname readeddata)
                     (rest pairs)))
            [(- index pos) result])))

      IWritableSpec
      (write [_ buff pos data']
        (let [written (reduce (fn [index [fieldname type]]
                                (let [value (get data' fieldname nil)
                                      written (write type buff index value)]
                                  (+ index written)))
                              pos data)]
          (- written pos))))))

(defmethod spec :indexed
  [& types]
  (reify
    clojure.lang.Counted
    (count [_]
      (count types))

    ISpecType
    (tag [_] :static)

    IStaticSize
    (size [_]
      (reduce #(+ %1 (proto/size %2)) 0 types))

    IReadableSpec
    (read [_ buff pos]
      (loop [index pos result [] types types]
        (if-let [type (first types)]
          (let [[readedbytes readeddata] (read type buff index)]
            (recur (+ index readedbytes)
                   (conj result readeddata)
                   (rest types)))
          [(- index pos) result])))

    IWritableSpec
    (write [_ buff pos data']
      (let [indexedtypes (map-indexed vector types)
            written (reduce (fn [pos [index type]]
                              (let [value (nth data' index nil)
                                    written (write type buff pos value)]
                                (+ pos written)))
                            pos indexedtypes)]
        (- written pos)))))

(def ^{:doc "A semantic alias for spec constructor."}
  compose-type spec)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn int32
  "Create a int32 indexed data type."
  ([] (int32 0))
  ([default]
   (reify
     ISpecType
     (tag [_] :static)

     IReadableSpec
     (read [_ buff pos]
       [(Integer/BYTES)
        (buffer/read-int buff pos)])

     IWritableSpec
     (write [_ buff pos value]
       (let [value (or value default)]
         (buffer/write-int buff pos value)
         (Integer/BYTES)))

     IStaticSize
     (size [_]
       (Integer/BYTES)))))

(defn int64
  "Create a int64 indexed data type."
  ([] (int64 0))
  ([default]
   (reify
     ISpecType
     (tag [_] :static)

     IReadableSpec
     (read [_ buff pos]
       [(Long/BYTES)
        (buffer/read-long buff pos)])

     IWritableSpec
     (write [_ buff pos value]
       (let [value (or value default)]
         (buffer/write-long buff pos value)
         (Long/BYTES)))

     IStaticSize
     (size [_]
       (Long/BYTES)))))

(defn string
  "Fixed size string spec constructor."
  [^long size]
  (reify
    ISpecType
    (tag [_] :static)

    IReadableSpec
    (read [_ buff pos]
      (let [rawdata (buffer/read-bytes buff pos size)
            length  (- size (bytes/zeropad-count rawdata))
            data (String. rawdata 0 length "UTF-8")]
        [size data]))

    IWritableSpec
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
        size))

    IStaticSize
    (size [_] size)))

(defn string*
  "Arbitrary size string spec constructor."
  []
  (reify
    ISpecType
    (tag [_] :dynamic)

    IReadableSpec
    (read [_ buff pos]
      (let [datasize (buffer/read-int buff pos)
            data (buffer/read-bytes buff (+ pos 4) datasize)
            data (String. data 0 datasize "UTF-8")]
        [(+ datasize 4) data]))

    IWritableSpec
    (write [_ buff pos value]
      (let [input (.getBytes value "UTF-8")
            length (count input)]
        (buffer/write-int buff pos length)
        (buffer/write-bytes buff (+ pos 4) length input)
        (+ length 4)))))
