(ns bytebuf.spec.basic
  (:refer-clojure :exclude [type read float double long short byte bytes])
  (:require [bytebuf.proto :as proto]
            [bytebuf.buffer :as buffer]
            [bytebuf.spec :as spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bool
  "Boolean type spec constructor."
  ([] (bool nil))
  ([default]
   (reify
     proto/IStaticSize
     (size [_] 1)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 1)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 2)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 4)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 8)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 4)

     spec/ISpec
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
     proto/IStaticSize
     (size [_] 8)

     spec/ISpec
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
    proto/IStaticSize
    (size [_] size)

    spec/ISpec
    (read [_ buff pos]
      [size (buffer/read-bytes buff pos size)])

    (write [_ buff pos value]
      (buffer/write-bytes buff pos size value)
      size)))
