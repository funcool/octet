(ns bytebuf.spec.basic
  (:refer-clojure :exclude [type read float double long short byte bytes])
  (:require [bytebuf.buffer :as buffer]
            [bytebuf.spec :as spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Boolean type spec."}
  bool
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
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
         1))))

(def ^{:doc "Byte type spec."}
  byte
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 1)

    spec/ISpec
    (read [_ buff pos]
      (let [readed (buffer/read-byte buff pos)]
        [1 readed]))

    (write [_ buff pos value]
      (some->> value (buffer/write-byte buff pos))
      1)))

(def ^{:doc "Short type spec."}
  int16
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 2)

    spec/ISpec
    (read [s buff pos]
      [(spec/size s)
       (buffer/read-short buff pos)])

    (write [s buff pos value]
      (some->> value (buffer/write-short buff pos))
      (spec/size s))))

(def ^{:doc "Integer type spec."}
  int32
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 4)

    spec/ISpec
    (read [s buff pos]
      [(spec/size s)
       (buffer/read-int buff pos)])

    (write [s buff pos value]
      (some->> value (buffer/write-int buff pos))
      (spec/size s))))

(def ^{:doc "Long type spec."}
  int64
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 8)

    spec/ISpec
    (read [s buff pos]
      [(spec/size s)
       (buffer/read-long buff pos)])

    (write [s buff pos value]
      (some->> value (buffer/write-long buff pos))
      (spec/size s))))

(def ^{:doc "Float type spec"}
  float
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 4)

    spec/ISpec
    (read [s buff pos]
      [(spec/size s)
       (buffer/read-float buff pos)])

    (write [s buff pos value]
      (some->> value (buffer/write-float buff pos))
      (spec/size s))))

(def ^{:doc "Double type spec."}
  double
  (reify
    #+clj
    clojure.lang.IFn
    #+clj
    (invoke [s] s)

    #+cljs
    IFn
    #+cljs
    (-invoke [s] s)

    spec/ISpecSize
    (size [_] 8)

    spec/ISpec
    (read [s buff pos]
      [(spec/size s)
       (buffer/read-double buff pos)])

    (write [s buff pos value]
      (some->> value (buffer/write-double buff pos))
      (spec/size s))))

(defn bytes
  "Fixed size byte array type spec constructor."
  [^long size]
  (reify
    spec/ISpecSize
    (size [_] size)

    spec/ISpec
    (read [_ buff pos]
      [size (buffer/read-bytes buff pos size)])

    (write [_ buff pos value]
      (buffer/write-bytes buff pos size value)
      size)))
