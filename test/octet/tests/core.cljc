;; Copyright 2015-2018 Andrey Antukh <niwi@niwi.nz>
;;
;; Licensed under the Apache License, Version 2.0 (the "License")
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns octet.tests.core
  (:require #?(:cljs [cljs.test :as t]
               :clj  [clojure.test :as t])
            [octet.core :as buf]
            [octet.buffer :as impl])
  #?(:clj
     (:import java.nio.ByteBuffer
              io.netty.buffer.ByteBuf)))

#?(:clj
   (do
     (defn random-bytes
       [^long numbytes]
       (let [sr (java.util.Random.)
             buffer (byte-array numbytes)]
         (.nextBytes sr buffer)
         buffer))

     (defn equals?
       [^bytes a ^bytes b]
       (java.util.Arrays/equals a b))

     (def bytes-type (Class/forName "[B"))

     (defn bytes->vecs [m]
       "test helper - byte arrays do not compare to
       (= a b) true, so we convert maps with byte
       array values to maps with vectors of bytes for
       comparison"
       (into {}
             (map (fn [[k v]]
                    (if (= (type v) bytes-type)
                      (vector k (apply (partial vector-of :byte) v))
                      (vector k v)))
                  m))))

   :cljs
   (do
     (defn random-bytes
       [^long numbytes]
       (let [buffer (js/Int8Array. numbytes)]
         (doseq [i (range numbytes)]
           (aset buffer i (rand-int 10)))
         buffer))

     (defn equals?
       [^bytes a ^bytes b]
       (if (not= (.-length a) (.-length b))
         false
         (reduce (fn [acc i]
                   (if (not= (aget a i) (aget b i))
                     (reduced false)
                     true))
                 true
                 (range (.-length a)))))))


#?(:clj
   (t/deftest allocate-heap-nio-buffer
     (let [buffer (buf/allocate 16)]
       (t/is (not (.isDirect buffer)))
       (t/is (instance? ByteBuffer buffer)))))

#?(:clj
   (t/deftest allocate-direct-nio-buffer
     (let [buffer (buf/allocate 16 {:type :direct})]
       (t/is (.isDirect buffer))
       (t/is (instance? ByteBuffer buffer)))))

#?(:clj
   (t/deftest allocate-heap-netty-buffer
     (let [buffer (buf/allocate 16 {:type :heap :impl :netty})]
       (t/is (not (.isDirect buffer)))
       (t/is (instance? ByteBuf buffer)))))

#?(:clj
   (t/deftest allocate-direct-netty-buffer
     (let [buffer (buf/allocate 16 {:type :direct :impl :netty})]
       (t/is (.isDirect buffer))
       (t/is (instance? ByteBuf buffer)))))

#?(:clj
   (t/deftest indexed-specs-write-with-offset
     (let [spec (buf/spec (buf/int32))
           buffer (buf/allocate 12)
           data [500]]
       (t/is (= (buf/write! buffer data spec {:offset 3}) 4))
       (t/is (= (.getInt buffer 3) 500)))))

#?(:cljs
   (t/deftest allocate-direct-es6-buffer
     (let [buffer (buf/allocate 16 {:impl :es6})]
       (t/is (instance? js/DataView buffer)))))

(t/deftest spec-constructor
  (let [spec (buf/spec :field1 (buf/int32)
                       :field2 (buf/int16))]
    (t/is (= (count spec) 2))
    (t/is (= (buf/size spec) 6))))

(t/deftest associative-specs-write
  (let [spec (buf/spec :field1 (buf/int32)
                       :field2 (buf/int16))
        buffer (buf/allocate 12)
        data {:field1 1 :field2 4}]
    (t/is (= (buf/write! buffer data spec) 6))
    (t/is (= (impl/read-int buffer 0) 1))
    (t/is (= (impl/read-short buffer 4) 4))))

(t/deftest associative-specs-write-with-offset
  (let [spec (buf/spec :field1 (buf/int32))
        buffer (buf/allocate 12)
        data {:field1 500}]
    (t/is (= (buf/write! buffer data spec {:offset 3}) 4))
    (t/is (= (impl/read-int buffer 3) 500))))

(t/deftest associative-specs-write-wrong-buffer
  ;; write data to wrong buffer (less space)
  (let [spec (buf/spec :field1 (buf/int32))
        buffer (buf/allocate 2)
        data {:field1 1}]

    #?(:clj
       (t/is (thrown? java.lang.IndexOutOfBoundsException
                      (buf/write! buffer data spec) 12))
       :cljs
       (t/is (thrown? js/Error
                      (buf/write! buffer data spec) 12)))))

(t/deftest associative-specs-read
  (let [spec (buf/spec :field1 (buf/int32)
                       :field2 (buf/int16))
        buffer (buf/allocate 12)]
    (impl/write-int buffer 0 10)
    (impl/write-short buffer 4 100)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 6))
      (t/is (= data {:field2 100 :field1 10})))))

(t/deftest associative-specs-read-with-offset
  (let [spec (buf/spec :field1 (buf/int32))
        buffer (buf/allocate 12)]
    (impl/write-int buffer 8 1000)
    (let [[readed data] (buf/read* buffer spec {:offset 8})]
      (t/is (= readed 4))
      (t/is (= data {:field1 1000})))))

(t/deftest indexed-specs-write
  (let [spec (buf/spec (buf/int32) (buf/int16))
        buffer (buf/allocate 12)
        data [1 4]]
    (t/is (= (buf/write! buffer data spec) 6))
    (t/is (= (impl/read-int buffer 0) 1))
    (t/is (= (impl/read-short buffer 4) 4))))

(t/deftest indexed-specs-write-wrong-buffer
  (let [spec (buf/spec (buf/int32))
        buffer (buf/allocate 2)
        data [1]]

    #?(:clj
       (t/is (thrown? java.lang.IndexOutOfBoundsException
                      (buf/write! buffer data spec) 12))
       :cljs
       (t/is (thrown? js/Error
                      (buf/write! buffer data spec) 12)))))

(t/deftest indexed-specs-read
  (let [spec (buf/spec (buf/int32) (buf/int16))
        buffer (buf/allocate 12)]
    (impl/write-int buffer 0 10)
    (impl/write-short buffer 4 100)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 6))
      (t/is (= data [10 100])))))

(t/deftest indexed-specs-read-with-offset
  (let [spec (buf/spec (buf/int32))
        buffer (buf/allocate 12)]
    (impl/write-int buffer 8 1000)
    (let [[readed data] (buf/read* buffer spec {:offset 8})]
      (t/is (= readed 4))
      (t/is (= data [1000])))))

#?(:clj
   (t/deftest spec-data-types
     (let [data [(buf/string 5) "12345"
                 (buf/short)    100
                 (buf/long)     1002
                 (buf/integer)  1001
                 (buf/bool)     false
                 (buf/uint16)   55000
                 (buf/uint32)   4294967295
                 (buf/ubyte)    (short 255)
                 (buf/double)   (double 4.3)
                 (buf/float)    (float 3.2)
                 (buf/uint64)   18446744073709551615N
                 (buf/byte)     (byte 32)]]
       ;; (buf/bytes 5)  (bytes/random-bytes 5)]]
       (doseq [[spec data] (partition 2 data)]
         (let [buffers [(buf/allocate (buf/size spec) {:type :heap :impl :nio})
                        (buf/allocate (buf/size spec) {:type :direct :impl :nio})
                        (buf/allocate (buf/size spec) {:type :heap :impl :netty})
                        (buf/allocate (buf/size spec) {:type :direct :impl :netty})]]
           (doseq [buffer buffers]
             (let [written (buf/write! buffer data spec)]
               (t/is (= written (buf/size spec)))
               (let [[readed data'] (buf/read* buffer spec)]
                 (t/is (= readed (buf/size spec)))
                 (t/is (= data data')))))))))
   :cljs
   (t/deftest spec-data-types
     (let [data [(buf/string 5) "äåéëþ"
                 (buf/short)    100
                 (buf/integer)  1001
                 (buf/uint16)   55000
                 (buf/uint32)   4294967295
                 (buf/ubyte)    (byte 255)
                 (buf/bool)     false
                 (buf/double)   (double 4.3)
                 (buf/float)    (float 3.5)
                 (buf/byte)     (byte 32)
                 (buf/bytes 5)  (random-bytes 5)]]
       (doseq [[spec data] (partition 2 data)]
         (let [buffers [(buf/allocate (buf/size spec) {:impl :es6})]]
           (doseq [buffer buffers]
             (let [written (buf/write! buffer data spec)]
               (t/is (= written (buf/size spec)))
               (let [[readed data'] (buf/read* buffer spec)]
                 (t/is (= readed (buf/size spec)))
                 (cond
                   (instance? js/Int8Array data')
                   (t/is (equals? data data'))
                   :else
                   (t/is (= data data')))))))))))

(t/deftest spec-data-with-cstring-single
  (let [spec (buf/spec (buf/cstring))
        buffer (buf/allocate 11)]
    (buf/write! buffer ["1234567890"] spec)
    (let [[readed data] (buf/read* buffer spec)]
        (t/is (= readed 11))
        (t/is (= data  ["1234567890"])))))

(t/deftest spec-data-with-dynamic-types-single
  (let [spec (buf/spec (buf/string*))
        buffer (buf/allocate 20)]
    (buf/write! buffer ["1234567890"] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 14))
      (t/is (= data ["1234567890"])))))

(t/deftest spec-data-with-dynamic-types-combined
  (let [spec (buf/spec (buf/string*) (buf/int32))
        buffer (buf/allocate 40)]
    (buf/write! buffer ["1234567890" 1000] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 18))
      (t/is (= data ["1234567890" 1000])))))

(t/deftest spec-data-with-cstring-and-other-types-combined
  (let [spec (buf/spec (buf/cstring) (buf/int32))
        buffer (buf/allocate 40)]
    (buf/write! buffer ["1234567890" 1000] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 15))
      (t/is (= data  ["1234567890" 1000])))))

(t/deftest spec-data-with-multi-cstring
  (let [spec (buf/spec buf/cstring buf/cstring)
        buffer (buf/allocate 40)]
    (buf/write! buffer ["1234567890" "1234567890"] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 22))
      (t/is (= data  ["1234567890" "1234567890"])))))

(t/deftest spec-data-with-types-and-cstring-combined
  (let [spec (buf/spec buf/cstring buf/int32 buf/byte buf/cstring)
        buffer (buf/allocate 40)]
    (buf/write! buffer ["1234567890" 1000  1 "1234567890"] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 27))
      (t/is (= data  ["1234567890" 1000  1 "1234567890"])))))

(t/deftest spec-data-with-multi-cstring-and-dynamic-types-combined
  (let [spec (buf/spec (buf/int32) (buf/cstring) (buf/cstring) (buf/int32))
        buffer (buf/allocate 40)]
    (buf/write! buffer [9999 "12345" "67890" 1000] spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 20))
      (t/is (= data  [9999 "12345" "67890" 1000])))))

(t/deftest spec-data-with-indexed-ref-string-single
  (let [spec (buf/spec (buf/int32)
                       (buf/int32)
                       (buf/int32)
                       (buf/ref-string 1))
        buffer (buf/allocate 17)
        result (buf/write! buffer [1 0 1 "12345"] spec)]
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 17))
      (t/is (= data  [1 5 1 "12345"])))))

(t/deftest spec-data-with-indexed-ref-string-lengths
  (let [lens [0 1 10 100 1000]
        spec (buf/spec (buf/int32)
                       (buf/int32)
                       (buf/int32)
                       (buf/ref-string 1))]
    (doseq [len lens]
      (let [str    (clojure.string/join (repeat len \x))
            total (+ 12 len)
            buffer (buf/allocate total)]
        (buf/write! buffer [1 0 1 str] spec)
        (let [[readed data] (buf/read* buffer spec)]
          (t/is (= readed total))
          (t/is (= data [1 len 1 str])))))))

(t/deftest spec-data-with-indexed-ref-string-interleaved
  (let [datas [[22 [0 1 0 3 "a",, 0 "xyz"]]
               [24 [9 3 7 3 "abc" 5 "xyz"]]
               [18 [0 0 0 0 "",,, 0 ""]]
               [20 [1 1 1 1 "a",, 1 "x"]]
               [21 [9 0 7 3 "",,, 9 "xyz"]]
               [21 [9 3 7 0 "abc" 5 ""]]]
        spec  (buf/spec (buf/int32)                         ;0
                        (buf/int32)                         ;1
                        (buf/int32)                         ;2
                        (buf/int32)                         ;3
                        (buf/ref-string 1)                  ;4
                        (buf/int16)                         ;5
                        (buf/ref-string 3))]                ;6
    (doseq [[count data] datas]
      (let [buffer (buf/allocate count)]
        (buf/write! buffer data spec)
        (let [[c d] (buf/read* buffer spec)]
          (t/is (= d data))
          (t/is (= c count)))))))

(t/deftest spec-data-with-assoc-ref-string-single
  (let [spec (buf/spec :bogus1 (buf/int32)
                       :length (buf/int32)
                       :bogus2 (buf/int32)
                       :varchar (buf/ref-string :length))
        buffer (buf/allocate 15)]
    (buf/write! buffer {:bogus1 1
                        :length 3
                        :bogus2 1
                        :varchar "123"} spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 15))
      (t/is (= data  {:bogus1 1
                      :length 3
                      :bogus2 1
                      :varchar "123"})))))

(t/deftest spec-data-with-assoc-ref-strings-interleaved
  (let [spec (buf/spec :bogus1  (buf/int32)
                       :length1 (buf/int32)
                       :bogus2  (buf/int32)
                       :length2 (buf/int32)
                       :varchar1 (buf/ref-string :length1)
                       :bogus3  (buf/int16)
                       :varchar2 (buf/ref-string :length2))
        buffer (buf/allocate 24)]
    (buf/write! buffer {:bogus1 12
                        :length1 3
                        :bogus2 23
                        :length2 3
                        :varchar1 "123"
                        :bogus3 34
                        :varchar2 "abc"} spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 24))
      (t/is (= data  {:bogus1 12
                      :length1 3
                      :bogus2 23
                      :length2 3
                      :varchar1 "123"
                      :bogus3 34
                      :varchar2 "abc"})))))

(t/deftest spec-data-with-assoc-ref-bytes-single
  (let [barr (random-bytes 3)
        spec (buf/spec :bogus1 (buf/int32)
                       :length (buf/int32)
                       :bogus2 (buf/int32)
                       :varbytes (buf/ref-bytes :length))
        buffer (buf/allocate 15)]
    (buf/write! buffer {:bogus1   1
                        :length   3
                        :bogus2   1
                        :varbytes barr} spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 15))
      (t/is (:bogus1 data) 1)
      (t/is (:length data) 3)
      (t/is (:bogus2 data) 1)
      (t/is (equals? (:varbytes data) barr)))))

(t/deftest spec-data-with-assoc-ref-bytes-interleaved
  (let [barr1 (random-bytes 3)
        barr2 (random-bytes 3)
        spec (buf/spec :bogus1 (buf/int32)
                       :length1 (buf/int32)
                       :bogus2 (buf/int32)
                       :length2 (buf/int32)
                       :varbytes1 (buf/ref-bytes :length1)
                       :bogus3 (buf/int16)
                       :varbytes2 (buf/ref-bytes :length2))
        buffer (buf/allocate 24)]
    (buf/write! buffer {:bogus1    12
                        :length1   3
                        :bogus2    23
                        :length2   3
                        :varbytes1 barr1
                        :bogus3    34
                        :varbytes2 barr2} spec)
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 24))
      (t/is (:bogus1 data) 12)
      (t/is (:length1 data) 3)
      (t/is (:bogus2 data) 23)
      (t/is (:length2 data) 3)
      (t/is (equals? (:varbytes1 data) barr1))
      (t/is (equals? (:varbytes2 data) barr2)))))

(t/deftest spec-composition
  (let [spec (buf/spec (buf/spec (buf/int32) (buf/int32))
                       (buf/spec (buf/string 10) (buf/string 5)))
        buffer (buf/allocate (buf/size spec))
        data [[100 200] ["foo" "bar"]]]
    (buf/write! buffer data spec)
    (let [[readed data'] (buf/read* buffer spec)]
      (t/is (= readed (buf/size spec)))
      (t/is (= data' data)))))

(t/deftest into-buffer
  (let [spec (buf/spec buf/int32 buf/int32)
        result (buf/into spec [1 3])]
    (t/is (= (impl/get-capacity result) 8)))
  (let [spec (buf/spec buf/string* buf/string*)
        result (buf/into spec ["hello" "world!"])]
    (t/is (= (impl/get-capacity result) 19))))

#?(:clj
   (t/deftest endianness
     (let [spec (buf/spec buf/int32 buf/int32)
           buff (buf/with-byte-order :little-endian
                  (buf/into spec [1 3]))
           res1 (buf/read buff spec)
           res2 (buf/with-byte-order :little-endian
                  (buf/read buff spec))]
       (t/is (= res1 [16777216 50331648]))
       (t/is (= res2 [1 3])))))

(t/deftest vector-buffer
  (let [spec (buf/spec buf/short buf/int32)
        buffers [(buf/allocate 2)
                 (buf/allocate 4)]]
    (buf/write! buffers [20 30] spec)
    (t/is (= (buf/read buffers spec) [20 30]))
    (t/is (= (buf/read (nth buffers 0) buf/short) 20))
    (t/is (= (buf/read (nth buffers 1) buf/int32) 30))))

(defrecord Point [x y])

(t/deftest spec-composition-with-compose
  (let [pointspec (buf/compose ->Point [buf/int32 buf/int32])
        point (->Point 1 2)
        buffer (buf/allocate 8)]
    (t/is (= 8 (buf/write! buffer point pointspec)))
    (t/is (= point (buf/read buffer pointspec)))))

(t/deftest spec-composition-with-repeat
  (let [spec (buf/repeat 5 buf/int32)
        specsize (buf/size spec)
        buffer (buf/allocate specsize)
        written (buf/write! buffer [1 2 3 4 5] spec)]
    (t/is (= written 20))
    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 20))
      (t/is (= data [1 2 3 4 5])))))

(t/deftest spec-data-with-dynamic-vector
  (let [spec (buf/vector* buf/int32)
        buffer (buf/into spec [1 2 3 4 5])
        written (impl/get-capacity buffer)]
    (t/is (= written 24))

    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 24))
      (t/is (= data [1 2 3 4 5])))))

(t/deftest spec-associative-nested-dynamic
  (let [spec (buf/spec :outer (buf/spec :inner (buf/vector* buf/int32)))
        buffer (buf/into (buf/spec :outer (buf/spec :inner (buf/vector* buf/int32))) {:outer {:inner [1]}})
        written (impl/get-capacity buffer)]
    (t/is (= written 8))

    (let [[readed data] (buf/read* buffer spec)]
      (t/is (= readed 8)
        (t/is (= data {:outer {:inner [1]}}))))))

#?(:cljs
   (do
     (enable-console-print!)
     (set! *main-cli-fn* #(t/run-tests))))

#?(:cljs
   (defmethod t/report [:cljs.test/default :end-run-tests]
     [m]
     (if (t/successful? m)
       (set! (.-exitCode js/process) 0)
       (set! (.-exitCode js/process) 1))))

