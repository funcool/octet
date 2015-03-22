(ns octet.core
  (:refer-clojure :exclude [read byte float double short long bytes into])
  (:require [octet.spec :as spec]
            [octet.spec.basic :as basic-spec]
            [octet.spec.string :as string-spec]
            [octet.buffer :as buffer]))

(def ^{:doc "Alias for `octet.spec/compose`."}
  compose spec/compose)

(def ^{:doc "Alias for `octet.spec/spec`."}
  spec spec/spec)

(def ^{:doc "Alias for `octet.spec/size`."}
  size spec/size)

(def ^{:doc "Fixed size string spec constructor."}
  string string-spec/string)

(def ^{:doc "Variable length string spec singleton instance."}
  string* string-spec/string*)

(def ^{:doc "Short spec instance."}
  int16 basic-spec/int16)

(def ^{:doc "Integer spec instance."}
  int32 basic-spec/int32)

(def ^{:doc "Long spec instance."}
  int64 basic-spec/int64)

(def ^{:doc "Float spec instance."}
  float basic-spec/float)

(def ^{:doc "Double spec instance."}
  double basic-spec/double)

(def ^{:doc "Byte spec instance."}
  byte basic-spec/byte)

(def ^{:doc "Fixed length byte array spec constructor."}
  bytes basic-spec/bytes)

(def ^{:doc "Boolean spec constructor."}
  bool basic-spec/bool)

(def ^{:doc (str "Polymorphic method for allocate new byte buffers. \n\n"
                 "Alias for `octer.buffer/allocate`.")}
  allocate buffer/allocate)

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
