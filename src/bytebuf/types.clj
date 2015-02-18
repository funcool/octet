(ns bytebuf.types
  (:refer-clojure :exclude [read])
  (:require [bytebuf.buffer :as buffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IDynamicType
  "Dynamic spec type mark.")

(defprotocol IStaticType
  "Static spec type mark.")

(defprotocol IReadableType
  (read [_ buff] "Read self corresponding data from the specified buffer."))

(defprotocol IWritableType
  (write [_ buff value] "Write value to the specified buffer."))

(defprotocol ITypeSize
  (size [_] [_ data] "Read the size of the type. In static type data is optional."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn int32
  "Create a int32 indexed data type."
  ([] (int32 0))
  ([default]
   (reify
     IStaticType
     IReadableType
     (read [_ buff]
       (buffer/read-integer buff))

     IWritableType
     (write [_ buff value]
       (let [value (or value default)]
         (.putInt buff value)))

    ITypeSize
    (size [_]
      (Integer/BYTES))
    (size [_ _]
      (Integer/BYTES)))))

(defn int64
  "Create a int64 indexed data type."
  ([] (int64 0))
  ([default]
   (reify
     IStaticType
     IReadableType
     (read [_ buff]
       (buffer/read-long buff))

     IWritableType
     (write [_ buff value]
       (let [value (or value default)]
         (.putLong buff value)))

     ITypeSize
     (size [_]
       (Long/BYTES))
     (size [_ _]
       (Long/BYTES)))))
