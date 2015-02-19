(ns bytebuf.proto
  "Generic protocols.")

(defprotocol IStaticSize
  (size [_] "Calculate the size in bytes of the object."))
