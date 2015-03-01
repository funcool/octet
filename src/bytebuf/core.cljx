(ns bytebuf.core
  (:refer-clojure :exclude [read byte float double short long bytes])
  (:require [bytebuf.spec :as spec]
            [bytebuf.spec.basic :as basic-spec]
            [bytebuf.spec.string :as string-spec]
            [bytebuf.buffer :as buffer]))

(def compose-type spec/compose-type)
(def spec spec/spec)
(def size spec/size)

(def string string-spec/string)
(def string* string-spec/string*)

(def int16 basic-spec/int16)
(def int32 basic-spec/int32)
(def int64 basic-spec/int64)
(def float basic-spec/float)
(def double basic-spec/double)
(def byte basic-spec/byte)
(def bytes basic-spec/bytes)
(def bool basic-spec/bool)

(def allocate buffer/allocate)

(def ^{:doc "Alias for int16"} short int16)
(def ^{:doc "Alias for int32"} integer int32)
(def ^{:doc "Alias for int64"} long int64)

(defn write!
  "Write data into buffer following the specified
  spec instance."
  ([buff data spec]
   (write! buff data spec {}))
  ([buff data spec {:keys [offset] :or {offset 0}}]
   #+clj
   (locking buff
     (spec/write spec buff offset data))
   #+cljs
   (do
     (spec/write spec buff offset data))))

(defn read*
  "Read data from buffer following the specified spec
  instance. This method returns a vector of readed data
  and the data itself.
  If you need only data, use `read` function."
  ([buff spec]
   (read* buff spec {}))
  ([buff spec {:keys [offset] :or {offset 0}}]
   #+clj
   (locking buff
     (spec/read spec buff offset))
   #+cljs
   (spec/read spec buff offset)))

(defn read
  "Read data from buffer following the specified
  spec instance. This function is a friend of `read*`
  and it returns only the readed data."
  [& args]
  (second (apply read* args)))
