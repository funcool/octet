(ns bytebuf.types
  (:refer-clojure :exclude [read])
  (:require [bytebuf.buffer :as buffer])
  (:import java.util.Arrays))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IType
  (tag [_] "Get the type tag."))

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
     IType
     (tag [_] :static)

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
     IType
     (tag [_] :static)

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


(defn string
  ([^long size]
   (reify
     IType
     (tag [_] :static)

     IReadableType
     (read [_ buff pos]
       (let [tmpbuf (byte-array size)]
         (.get buff tmpbuf)
         (println 1111 buff)
         (.position buff (- (.position buff) size))
         (println 2222 buff)
         [(String. tmpbuf "UTF-8") size]))

     IWritableType
     (write [_ buff pos value]
       (let [input (.getBytes value "UTF-8")
             length (count input)
             tmpbuf (byte-array size)]
         (System/arraycopy input 0 tmpbuf 0 length)
         (when (< length size)
           (Arrays/fill tmpbuf length size (byte 0)))

         (println 444 (vec input))
         (println 555 (vec tmpbuf))
         (.put buff tmpbuf)
         (println 666 buff)
         (.position buff (- (.position buff) size))
         (println 777 buff)

         size))

     ITypeSize
     (size [_] size)
     (size [_ _] size))))
