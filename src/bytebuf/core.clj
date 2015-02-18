(ns bytebuf.core
  (:require [bytebuf.types]
            [bytebuf.spec]
            [bytebuf.buffer]
            [potemkin.namespaces :refer [import-vars]]))

(import-vars
 [bytebuf.spec
  spec]
 [bytebuf.types
  int32
  int64]
 [bytebuf.buffer
  allocate])
