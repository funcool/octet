(ns bytebuf.core
  (:refer-clojure :exclude [read byte float double short long])
  (:require [bytebuf.spec :as spec]
            [bytebuf.buffer :as buffer]
            [bytebuf.proto :as proto]
            [potemkin.namespaces :refer [import-vars]]))

(import-vars
 [bytebuf.spec
  compose-type
  spec
  string
  string*
  int32
  int64
  short
  integer
  long
  float
  double
  real32
  real64
  byte
  bool]
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
