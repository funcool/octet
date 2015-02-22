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

(deftest associative-static-specs
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


;; (deftest experiments
;;   (testing "Experiment with string."
;;     (let [spec (buf/spec :f1 (buf/string 10))
;;           buffer (buf/allocate 20)]
;;       (buf/write! buffer {:f1 "kaka"} spec)
;;       (let [[readed data] (buf/read* buffer spec)]
;;         (println 111 readed)
;;         (println 111 data))))
;; )
