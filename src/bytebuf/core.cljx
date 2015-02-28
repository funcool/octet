(ns bytebuf.core
  (:refer-clojure :exclude [read byte float double short long bytes])
  (:require [bytebuf.spec :as spec]
            [bytebuf.buffer :as buffer]
            [bytebuf.proto :as proto]))

(def compose-type spec/compose-type)
(def spec spec/spec)
;; (def string spec/string)
;; (def string* spec/string*)
(def int32 spec/int32)
(def int64 spec/int64)
(def short spec/short)
(def integer spec/integer)
(def long spec/long)
(def float spec/float)
(def double spec/double)
(def byte spec/byte)
(def bytes spec/bytes)
(def bool spec/bool)
(def allocate buffer/allocate)
(def size proto/size)

(defn write!
  "Write data into buffer following the specified
  spec instance."
  ([buff data spec]
   (write! buff data spec {}))
  ([buff data spec {:keys [offset] :or {offset 0}}]
   (locking buff
     (spec/write spec buff offset data))))

(defn read*
  "Read data from buffer following the specified spec
  instance. This method returns a vector of readed data
  and the data itself.
  If you need only data, use `read` function."
  ([buff spec]
   (read* buff spec {}))
  ([buff spec {:keys [offset] :or {offset 0}}]
   (locking buff
     (spec/read spec buff offset))))

(defn read
  "Read data from buffer following the specified
  spec instance. This function is a friend of `read*`
  and it returns only the readed data."
  [& args]
  (second (apply read* args)))
