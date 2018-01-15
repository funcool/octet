;; Copyright (c) 2015-2018 Andrey Antukh <niwi@niwi.nz>
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

(ns octet.spec.reference
  "Spec types for arbitrary length byte arrays/strings with a length reference."
  (:require [octet.buffer :as buffer]
            [octet.spec :as spec]
            [octet.spec.basic :as basic-spec]
            [octet.spec.string :as string-spec]))


(defn- ref-size
  [type data]
  (cond
    (satisfies? spec/ISpecDynamicSize type)
    (spec/size* type data)

    (satisfies? spec/ISpecSize type)
    (spec/size type)

    :else
    (throw (ex-info "Unexpected type" {:type type}))))

(defn- coerce-types
  "to make it terser to handle both maps (assoc spec) and seq/vector
  (indexed spec), we coerce the seq/vector types to maps where the
  keys are indexes"
  [types]
  (cond
    (map? types)
    types

    (or (seq? types)
        (vector? types))
    (apply array-map (interleave (range) types))

    :else (throw (ex-info "invalid type structure, not map, seq or vector"
                          {:type-structure types}))))

(defn- ref-len-offset
  "for ref-xxxx specs, calculate the byte offset of the
  spec containing the length, i.e. (spec :a (int16) :b (int16) c: (ref-string* :b))
  would cause this method to be called with :b (since the ref-string has a :b
  reference as its length) and should then return 2 as the second int16 is at byte offset 2"
  [ref-kw-or-index types data]
  (reduce-kv
   (fn [acc kw-or-index type]
     (if (= ref-kw-or-index kw-or-index)
       (reduced acc)
       (+ acc (ref-size type (get data kw-or-index)))))
   0
   types))

(defn- ref-write-length
  [type buff offset length]
  (cond
    (identical? basic-spec/int16 type)
    (buffer/write-short buff offset length)

    (identical? basic-spec/int32 type)
    (buffer/write-int buff offset length)

    (identical? basic-spec/int64 type)
    (buffer/write-long buff offset length)

    (identical? basic-spec/int64 type)
    (buffer/write-long buff offset length)

    (identical? basic-spec/uint16 type)
    (buffer/write-ushort buff offset length)

    (identical? basic-spec/uint32 type)
    (buffer/write-uint buff offset length)

    (identical? basic-spec/uint64 type)
    (buffer/write-ulong buff offset length)

    :else
    (throw (ex-info "Invalid reference type: should be int16, int32, int64 and unsigned variants" {}))))

(defn- ref-write
  "write a ref spec, will also write the length to the length spec"
  [ref-kw-or-index buff pos value types data]
  (let [input      (if (string? value) (string-spec/string->bytes value) value)
        length     (count input)
        types      (coerce-types types)
        len-offset (ref-len-offset ref-kw-or-index types data)
        len-type   (get types ref-kw-or-index)]
    (ref-write-length len-type buff len-offset length)
    (buffer/write-bytes buff pos length input)
    (+ length)))

(defn- ref-read
  "read ref spec, will read the length from the length spec"
  [ref-kw-or-index buff pos parent]
  (let [datasize (cond
                   (map? parent)
                   (ref-kw-or-index parent)

                   (or (seq? parent) (vector? parent))
                   (get parent ref-kw-or-index)

                   :else
                   (throw (ex-info
                           (str "bad ref-string/ref-bytes length reference  - " ref-kw-or-index)
                           {:length-kw ref-kw-or-index
                            :data-read parent})))
        ;; _ (println "FOFOFO:" parent ref-kw-or-index)
        data     (buffer/read-bytes buff pos datasize)]
    [datasize data]))

(defn ref-bytes
  "create a dynamic length byte array where the length of the byte array is stored in another
  spec within the containing indexed spec or associative spec. Example usages:
  (spec (int16) (int16) (ref-bytes* 1))
  (spec :a (int16) :b (int32) (ref-bytes* :b))
  where the first example would store the length of the byte array in the second int16 and
  the second example would store the length of the byte array in the int32 at key :b."
  [ref-kw-or-index]
  (reify
    #?@(:clj
        [clojure.lang.IFn
         (invoke [s] s)]
        :cljs
        [cljs.core/IFn
         (-invoke [s] s)])

    spec/ISpecDynamicSize
    (size* [_ data]
      (count data))

    spec/ISpecWithRef
    (read* [_ buff pos parent]
      (ref-read ref-kw-or-index buff pos parent))

    (write* [_ buff pos value types data]
      (ref-write ref-kw-or-index buff pos value types data))))

(defn ref-string
  "create a dynamic length string  where the length of the string is stored in another
  spec within the containing indexed spec or associative spec. Example usages:
  (spec (int16) (int16) (ref-string* 1))
  (spec :a (int16) :b (int32) (ref-string* :b))
  where the first example would store the length of the string in the second int16 and
  the second example would store the length of the string in the int32 at key :b."
  [ref-kw-or-index]
  (reify
    #?@(:clj
        [clojure.lang.IFn
         (invoke [s] s)]
        :cljs
        [cljs.core/IFn
         (-invoke [s] s)])

    spec/ISpecDynamicSize
    (size* [_ data]
      (let [data (string-spec/string->bytes data)]
        (count data)))

    spec/ISpecWithRef
    (read* [_ buff pos parent]
      (let [[datasize bytes] (ref-read ref-kw-or-index buff pos parent)]
        [datasize (string-spec/bytes->string bytes datasize)]))

    (write* [_ buff pos value types data]
      (ref-write ref-kw-or-index buff pos value types data))))
