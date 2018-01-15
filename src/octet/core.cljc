;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;
;; * Redistributions of source code must retain the above copyright notice, this
;;   list of conditions and the following disclaimer.
;;
;; * Redistributions in binary form must reproduce the above copyright notice,
;;   this list of conditions and the following disclaimer in the documentation
;;   and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
;; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
;; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns octet.core
  (:refer-clojure :exclude [read byte float double short long bytes into repeat])
  (:require [octet.spec :as spec]
            #?(:cljs
               [octet.util :as util :include-macros true]
               :clj
               [octet.util :as util])
            [octet.spec.basic :as basic-spec]
            [octet.spec.string :as string-spec]
            [octet.spec.collections :as coll-spec]
            [octet.spec.reference :as ref-spec]
            [octet.buffer :as buffer]))

(util/defalias compose spec/compose)
(util/defalias spec spec/spec)
(util/defalias size spec/size)
(util/defalias repeat spec/repeat)
(util/defalias string string-spec/string)
(util/defalias string* string-spec/string*)
(util/defalias vector* coll-spec/vector*)
(util/defalias ref-string ref-spec/ref-string)
(util/defalias ref-bytes ref-spec/ref-bytes)
(util/defalias int16 basic-spec/int16)
(util/defalias uint16 basic-spec/uint16)
(util/defalias int32 basic-spec/int32)
(util/defalias uint32 basic-spec/uint32)
(util/defalias int64 basic-spec/int64)
(util/defalias uint64 basic-spec/uint64)
(util/defalias float basic-spec/float)
(util/defalias double basic-spec/double)
(util/defalias byte basic-spec/byte)
(util/defalias ubyte basic-spec/ubyte)
(util/defalias bytes basic-spec/bytes)
(util/defalias bool basic-spec/bool)
(util/defalias allocate buffer/allocate)
(util/defalias get-capacity buffer/get-capacity)
(util/defalias short int16)
(util/defalias integer int32)
(util/defalias long int64)

(defn write!
  "Write data into buffer following the specified
  spec instance."
  ([buff data spec]
   (write! buff data spec {}))
  ([buff data spec {:keys [offset] :or {offset 0}}]
   #?(:clj
      (locking buff
        (spec/write spec buff offset data))
      :cljs
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
   #?(:clj (locking buff
             (spec/read spec buff offset))
      :cljs (spec/read spec buff offset))))

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

#?(:clj
   (defmacro with-byte-order
     [byteorder & body]
     `(binding [buffer/*byte-order* ~byteorder]
        ~@body)))
