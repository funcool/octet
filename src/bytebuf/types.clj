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
  (read [_ buff pos] "Read self corresponding data from the specified buffer."))

(defprotocol IWritableType
  (write [_ buff pos value] "Write value to the specified buffer and return the written bytes."))

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
     (read [_ buff pos]
       [(buffer/read-int buff pos)
        (Integer/BYTES)])

     IWritableType
     (write [_ buff pos value]
       (let [value (or value default)]
         (buffer/write-int buff pos value)
         (Integer/BYTES)))

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
     (read [_ buff pos]
       [(buffer/read-long buff pos)
        (Long/BYTES)])

     IWritableType
     (write [_ buff pos value]
       (let [value (or value default)]
         (buffer/write-long buff pos value)
         (Long/BYTES)))

     ITypeSize
     (size [_]
       (Long/BYTES))
     (size [_ _]
       (Long/BYTES)))))
