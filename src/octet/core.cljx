(ns octet.core
  (:refer-clojure :exclude [read byte float double short long bytes into repeat])
  #+cljs (:require-macros [octet.util :refer [defalias]])
  (:require [octet.spec :as spec]
            #+clj [octet.util :refer [defalias]]
            [octet.spec.basic :as basic-spec]
            [octet.spec.string :as string-spec]
            [octet.buffer :as buffer]))

(defalias compose spec/compose)
(defalias spec spec/spec)
(defalias size spec/size)
(defalias repeat spec/repeat)
(defalias string string-spec/string)
(defalias string* string-spec/string*)
(defalias int16 basic-spec/int16)
(defalias int32 basic-spec/int32)
(defalias int64 basic-spec/int64)
(defalias float basic-spec/float)
(defalias double basic-spec/double)
(defalias byte basic-spec/byte)
(defalias bytes basic-spec/bytes)
(defalias bool basic-spec/bool)
(defalias allocate buffer/allocate)
(defalias short int16)
(defalias integer int32)
(defalias long int64)

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
  instance.

  This method returns a vector of readed data
  and the data itself. If you need only data,
  use `read` function instead."
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

(defn into
  "Returns a buffer of exact size of
  spec with data already serialized."
  ([spec data] (into spec data {}))
  ([spec data opts]
   (let [size (spec/size* spec data)
         buffer (buffer/allocate size opts)]
     (write! buffer data spec opts)
     buffer)))
