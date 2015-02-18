(ns bytebuf.buffer
  "Buffer abstractions."
  (:import java.nio.ByteBuffer
           io.netty.buffer.ByteBuf
           io.netty.buffer.ByteBufAllocator))


(defprotocol IBuffer
  (read-integer* [_] "Read an integer (32 bits) from buffer.")
  (read-long* [_] "Read an long (64 bits) from buffer.")
  (tell* [_] "Get the current position index of buffer.")
  (seek* [_ val] "Set the current position index on buffer."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NIO ByteBuffer implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type ByteBuffer
  IBuffer
  (read-integer* [buff]
    (.getInt buff))
  (read-long* [buff]
    (.getLong buff))
  (tell* [buff]
    (.position buff))
  (seek* [buff val]
    (.position buff val)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:private true}
  allocator (ByteBufAllocator/DEFAULT))

(defn allocate
  ([size] (allocate size {}))
  ([size {:keys [type impl] :or {type :heap impl :nio}}]
   (case impl
     :nio (case type
            :heap (ByteBuffer/allocate size)
            :direct (ByteBuffer/allocateDirect size))
     :netty (case type
              :heap (.heapBuffer allocator size)
              :direct (.directBuffer allocator size)))))

(defn seek!
  "Set the position index on the buffer."
  [buff ^long pos]
  (seek! buff pos))
