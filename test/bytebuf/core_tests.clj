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

(ns bytebuf.core-tests
  (:require [clojure.test :refer :all]
            [bytebuf.core :as buf])
  (:import java.nio.ByteBuffer
           io.netty.buffer.ByteBuf))

(deftest allocate
  (testing "Allocate heap nio buffer"
    (let [buffer (buf/allocate 16)]
      (is (not (.isDirect buffer)))
      (is (instance? ByteBuffer buffer))))

  (testing "Allocate direct nio buffer"
    (let [buffer (buf/allocate 16 {:type :direct})]
      (is (.isDirect buffer))
      (is (instance? ByteBuffer buffer))))

  (testing "Allocate heap netty buffer"
    (let [buffer (buf/allocate 16 {:type :heap :impl :netty})]
      (is (not (.isDirect buffer)))
      (is (instance? ByteBuf buffer))))

  (testing "Allocate direct netty buffer"
    (let [buffer (buf/allocate 16 {:type :direct :impl :netty})]
      (is (.isDirect buffer))
      (is (instance? ByteBuf buffer))))
)

(deftest spec-constructor
  (testing "creating spec"
    (let [spec (buf/spec :field1 (buf/int32)
                         :field2 (buf/int64))]
      (is (= (count spec) 2))
      (is (= (buf/size spec) 12))))
)

(deftest associative-specs
  (testing "write data"
    (let [spec (buf/spec :field1 (buf/int32)
                         :field2 (buf/int64))
          buffer (buf/allocate 12)
          data {:field1 1 :field2 4}]
      (is (= (buf/write! buffer data spec) 12))
      (is (= (.getInt buffer 0) 1))
      (is (= (.getLong buffer 4) 4))))

  (testing "write data with offset"
    (let [spec (buf/spec :field1 (buf/int32))
          buffer (buf/allocate 12)
          data {:field1 500}]
      (is (= (buf/write! buffer data spec {:offset 3}) 4))
      (is (= (.getInt buffer 3) 500))))

  (testing "write data to wrong buffer (less space)"
    (let [spec (buf/spec :field1 (buf/int32))
          buffer (buf/allocate 2)
          data {:field1 1}]
      (is (thrown? java.lang.IndexOutOfBoundsException
                   (buf/write! buffer data spec) 12))))

  (testing "read data"
    (let [spec (buf/spec :field1 (buf/int32)
                         :field2 (buf/int64))
          buffer (buf/allocate 12)]
      (.putInt buffer 0 10)
      (.putLong buffer 4 100)
      (let [[readed data] (buf/read* buffer spec)]
        (is (= readed 12))
        (is (= data {:field2 100 :field1 10})))))

  (testing "read data with offset"
    (let [spec (buf/spec :field1 (buf/int32))
          buffer (buf/allocate 12)]
      (.putInt buffer 8 1000)
      (let [[readed data] (buf/read* buffer spec {:offset 8})]
        (is (= readed 4))
        (is (= data {:field1 1000})))))
  )

(deftest indexed-specs
  (testing "write data"
    (let [spec (buf/spec (buf/int32) (buf/int64))
          buffer (buf/allocate 12)
          data [1 4]]
      (is (= (buf/write! buffer data spec) 12))
      (is (= (.getInt buffer 0) 1))
      (is (= (.getLong buffer 4) 4))))

  (testing "write data with offset"
    (let [spec (buf/spec (buf/int32))
          buffer (buf/allocate 12)
          data [500]]
      (is (= (buf/write! buffer data spec {:offset 3}) 4))
      (is (= (.getInt buffer 3) 500))))

  (testing "write data to wrong buffer (less space)"
    (let [spec (buf/spec (buf/int32))
          buffer (buf/allocate 2)
          data [1]]
      (is (thrown? java.lang.IndexOutOfBoundsException
                   (buf/write! buffer data spec) 12))))

  (testing "read data"
    (let [spec (buf/spec (buf/int32) (buf/int64))
          buffer (buf/allocate 12)]
      (.putInt buffer 0 10)
      (.putLong buffer 4 100)
      (let [[readed data] (buf/read* buffer spec)]
        (is (= readed 12))
        (is (= data [10 100])))))

  (testing "read data with offset"
    (let [spec (buf/spec (buf/int32))
          buffer (buf/allocate 12)]
      (.putInt buffer 8 1000)
      (let [[readed data] (buf/read* buffer spec {:offset 8})]
        (is (= readed 4))
        (is (= data [1000])))))
  )

(deftest spec-data-types
  (testing "read/write static string"
    (let [spec (buf/spec (buf/string 5))
          buffer (buf/allocate 20)]
      (buf/write! buffer ["1234567890"] spec)
      (let [[readed data] (buf/read* buffer spec)]
        (is (= readed 5))
        (is (= data ["12345"])))))

  (testing "read/write float and double"
    (let [spec (buf/spec (buf/float) (buf/double))
          buffer (buf/allocate (buf/size spec))
          data [(float 1.2) (double 3.55)]]
      (buf/write! buffer data spec)
      (let [[readed data'] (buf/read* buffer spec)]
        (is (= readed (buf/size spec)))
        (is (= data data')))))

  (testing "read/write boolean and byte"
    (let [spec (buf/spec (buf/bool) (buf/byte))
          buffer (buf/allocate (buf/size spec))
          data [true (byte 22)]]
      (buf/write! buffer data spec)
      (let [[readed data'] (buf/read* buffer spec)]
        (is (= readed (buf/size spec)))
        (is (= data data')))))
  )

(deftest spec-data-with-dynamic-types
  (testing "read/write dynamic string"
    (let [spec (buf/spec (buf/string*))
          buffer (buf/allocate 20)]
      (buf/write! buffer ["1234567890"] spec)
      (let [[readed data] (buf/read* buffer spec)]
        (is (= readed 14))
        (is (= data ["1234567890"])))))

  (testing "read/write dynamic string combined with other types"
    (let [spec (buf/spec (buf/string*) (buf/int64))
          buffer (buf/allocate 40)]
      (buf/write! buffer ["1234567890" 1000] spec)
      (let [[readed data] (buf/read* buffer spec)]
        (is (= readed 22))
        (is (= data ["1234567890" 1000])))))
  )

(deftest spec-composition
  (testing "read/write a indexed spec composed with two indexed specs"
    (let [spec (buf/spec (buf/compose-type (buf/int32) (buf/int32))
                         (buf/compose-type (buf/string 10) (buf/string 5)))
          buffer (buf/allocate (buf/size spec))
          data [[100 200] ["foo" "bar"]]]
      (buf/write! buffer data spec)
      (let [[readed data'] (buf/read* buffer spec)]
        (is (= readed (buf/size spec)))
        (is (= data' data)))))
  )
