(ns bytebuf.core
  (:refer-clojure :exclude [read])
  (:require [bytebuf.types :as types]
            [bytebuf.spec :as spec]
            [bytebuf.buffer :as buffer]
            [bytebuf.proto :as proto]
            [potemkin.namespaces :refer [import-vars]]))

(import-vars
 [bytebuf.spec
  spec]
 [bytebuf.types
  string
  int32
  int64]
 [bytebuf.buffer
  allocate]
 [bytebuf.proto
  size])

(defn write!
  ([buff data spec]
   (write! buff data spec {}))
  ([buff data spec {:keys [offset] :or {offset 0}}]
   (locking buff
     (spec/write spec buff offset data))))

(defn read*
  ([buff spec]
   (read* buff spec {}))
  ([buff spec {:keys [offset] :or {offset 0}}]
   (locking buff
     (spec/read spec buff offset))))

(defn read
  [& args]
  (second (apply read* args)))
