;; Copyright 2015 Andrey Antukh <niwi@niwi.be>
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
  (:require #+clj [clojure.test :as t]
            #+cljs [cljs.test :as t]
            #+cljs [cljs-testrunners.node :as node]
            [octet.core :as buf]
            [octet.buffer :as impl])
  #+clj
  (:import java.nio.ByteBuffer
           io.netty.buffer.ByteBuf))

#+clj
(defn random-bytes
  [^long numbytes]
  (let [sr (java.util.Random.)
        buffer (byte-array numbytes)]
    (.nextBytes sr buffer)
    buffer))

#+cljs
(defn random-bytes
  [^long numbytes]
  (let [buffer (js/Int8Array. numbytes)]
    (doseq [i (range numbytes)]
      (aset buffer i (rand-int 10)))
    buffer))

#+clj
(defn equals?
  "Check if two byte arrays are equals."
  [^bytes a ^bytes b]
  (java.util.Arrays/equals a b))

#+cljs
(defn equals?
  [^bytes a ^bytes b]
  (if (not= (.-length a) (.-length b))
    false
    (reduce (fn [acc i]
              (if (not= (aget a i) (aget b i))
                (reduced false)
                true))
            true
            (range (.-length a)))))

#+clj
(t/deftest allocate-heap-nio-buffer
  (let [buffer (buf/allocate 16)]
    (t/is (not (.isDirect buffer)))
    (t/is (instance? ByteBuffer buffer))))

#+clj
(t/deftest allocate-direct-nio-buffer
  (let [buffer (buf/allocate 16 {:type :direct})]
    (t/is (.isDirect buffer))
    (t/is (instance? ByteBuffer buffer))))

#+clj
(t/deftest allocate-heap-netty-buffer
  (let [buffer (buf/allocate 16 {:type :heap :impl :netty})]
    (t/is (not (.isDirect buffer)))
    (t/is (instance? ByteBuf buffer))))

#+clj
(t/deftest allocate-direct-netty-buffer
  (let [buffer (buf/allocate 16 {:type :direct :impl :netty})]
    (t/is (.isDirect buffer))
    (t/is (instance? ByteBuf buffer))))

#+cljs
(t/deftest allocate-direct-es6-buffer
  (let [buffer (buf/allocate 16 {:impl :es6})]
    (t/is (instance? js/DataView buffer))))

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
    #+clj
    (t/is (thrown? java.lang.IndexOutOfBoundsException
                   (buf/write! buffer data spec) 12))
    #+cljs
    (t/is (thrown? js/Error
                   (buf/write! buffer data spec) 12))))

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

#+clj
(t/deftest indexed-specs-write-with-offset
  (let [spec (buf/spec (buf/int32))
        buffer (buf/allocate 12)
        data [500]]
    (t/is (= (buf/write! buffer data spec {:offset 3}) 4))
    (t/is (= (.getInt buffer 3) 500))))


(t/deftest indexed-specs-write-wrong-buffer
  (let [spec (buf/spec (buf/int32))
        buffer (buf/allocate 2)
        data [1]]
    #+clj
    (t/is (thrown? java.lang.IndexOutOfBoundsException
                   (buf/write! buffer data spec) 12))
    #+cljs
    (t/is (thrown? js/Error
                   (buf/write! buffer data spec) 12))))

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

#+clj
(t/deftest spec-data-types
  (let [data [(buf/string 5) "12345"
              (buf/short)    100
              (buf/long)     1002
              (buf/integer)  1001
              (buf/bool)     false
              (buf/double)   (double 4.3)
              (buf/float)    (float 3.2)
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

#+cljs
(t/deftest spec-data-types
  (let [data [(buf/string 5) "äåéëþ"
              (buf/short)    100
              (buf/integer)  1001
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
                (t/is (= data data'))))))))))

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

(t/deftest spec-composition
  (let [spec (buf/spec (buf/spec (buf/int32) (buf/int32))
                       (buf/spec (buf/string 10) (buf/string 5)))
        buffer (buf/allocate (buf/size spec))
        data [[100 200] ["foo" "bar"]]]
    (buf/write! buffer data spec)
    (let [[readed data'] (buf/read* buffer spec)]
      (t/is (= readed (buf/size spec)))
      (t/is (= data' data)))))

(defrecord Point [x y])

(t/deftest spec-composition-with-compose
  (let [pointspec (buf/compose ->Point [buf/int32 buf/int32])
        point (->Point 1 2)
        buffer (buf/allocate 8)]
    (t/is (= 8 (buf/write! buffer point pointspec)))
    (t/is (= point (buf/read buffer pointspec)))))

(t/deftest into-buffer
  (let [spec (buf/spec buf/int32 buf/int32)
        result (buf/into spec [1 3])]
    (t/is (= (impl/get-capacity result) 8)))

  (let [spec (buf/spec buf/string* buf/string*)
        result (buf/into spec ["hello" "world!"])]
    (t/is (= (impl/get-capacity result) 19))))

(t/deftest vector-buffer
  (let [spec (buf/spec buf/short buf/int32)
        buffers [(buf/allocate 2)
                 (buf/allocate 4)]]
    (buf/write! buffers [20 30] spec)
    (t/is (= (buf/read buffers spec) [20 30]))
    (t/is (= (buf/read (nth buffers 0) buf/short) 20))
    (t/is (= (buf/read (nth buffers 1) buf/int32) 30))))


#+cljs (set! *main-cli-fn* #(node/run-tests))
