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

(ns octet.spec.basic
  (:refer-clojure :exclude [type read float double long short byte bytes])
  (:require [octet.buffer :as buffer]
            [octet.spec :as spec]))

;; --- Types implementation

(defn primitive-spec
  [size readfn writefn]
  (reify
    #?@(:clj
        [clojure.lang.IFn
         (invoke [s] s)]
        :cljs
        [cljs.core/IFn
         (-invoke [s] s)])

    spec/ISpecSize
    (size [_] size)

    spec/ISpec
    (read [_ buff pos]
      (let [readed (readfn buff pos)]
        [size readed]))

    (write [_ buff pos value]
      (some->> value (writefn buff pos))
      size)))

(def ^{:doc "Boolean type spec."}
  bool
  (reify
    #?@(:clj
        [clojure.lang.IFn
         (invoke [s] s)]
        :cljs
        [cljs.core/IFn
         (-invoke [s] s)])

    spec/ISpecSize
    (size [_#] 1)

    spec/ISpec
    (read [_# buff pos]
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
  byte (primitive-spec 1 buffer/read-byte buffer/write-byte))

(def ^{:doc "Short type spec."}
  int16 (primitive-spec 2 buffer/read-short buffer/write-short))

(def ^{:doc "Integer type spec."}
  int32 (primitive-spec 4 buffer/read-int buffer/write-int))

(def ^{:doc "Long type spec."}
  int64 (primitive-spec 8 buffer/read-long buffer/write-long))

(def ^{:doc "Float type spec"}
  float (primitive-spec 4 buffer/read-float buffer/write-float))

(def ^{:doc "Double type spec."}
  double (primitive-spec 8 buffer/read-double buffer/write-double))

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

;; --- Unsigned Primitives

(def ^{:doc "Unsinged int16 type spec."}
  uint16 (primitive-spec 2 buffer/read-ushort buffer/write-ushort))

(def ^{:doc "Unsinged int32 type spec."}
  uint32 (primitive-spec 4 buffer/read-uint buffer/write-uint))

(def ^{:doc "Unsinged byte type spec."}
  ubyte (primitive-spec 1 buffer/read-ubyte buffer/write-ubyte))

(def ^{:doc "Unsinged int64 type spec."}
  uint64 (primitive-spec 8 buffer/read-ulong buffer/write-ulong))
