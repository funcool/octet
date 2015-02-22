(ns bytebuf.buffer
  "Buffer abstractions."
  (:import java.nio.ByteBuffer
           io.netty.buffer.ByteBuf
           io.netty.buffer.ByteBufAllocator))


(defprotocol IBuffer
  (read-int [_ pos] "Read an integer (32 bits) from buffer.")
  (write-int [_ pos value] "Write an integer to the buffer.")
  (read-long [_ pos] "Read an long (64 bits) from buffer.")
  (write-long [_ pos value] "Write an long to the buffer.")
  (read-bytes [_ pos size] "Read a byte array.")
  (write-bytes [_ pos size data] "Write byte array."))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NIO & Netty Buffer implementations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol IBuffer
  ByteBuffer
  (read-int [buff pos]
    (.getInt buff pos))
  (read-long [buff pos]
    (.getLong buff pos))
  (write-int [buff pos value]
    (.putInt buff pos value))
  (write-long [buff pos value]
    (.putLong buff pos value))
  (read-bytes [buff pos size]
    (let [tmpbuf (byte-array size)
          oldpos (.position buff)]
      (.position buff pos)
      (.get buff tmpbuf)
      (.position buff oldpos)
      tmpbuf))
  (write-bytes [buff pos size data]
    (let [oldpos (.position buff)]
      (.position buff pos)
      (.put buff data 0 size)
      (.position buff oldpos)))

  ByteBuf
  (read-int [buff pos]
    (.getInt buff pos))
  (read-long [buff pos]
    (.getLong buff pos))
  (write-int [buff pos value]
    (.setInt buff pos value))
  (write-long [buff pos value]
    (.setLong buff pos value))
  (read-bytes [buff pos size]
    (let [tmpbuf (byte-array size)]
      (.getBytes buff tmpbuf)
      tmpbuf))
  (write-bytes [buff pos size data]
    (.setBytes buff pos 0 size)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:private true}
  allocator (ByteBufAllocator/DEFAULT))

(defmulti allocate
  (fn [size & [{:keys [type impl] :or {type :heap impl :nio}}]]
    [type impl]))

(defmethod allocate [:heap :nio]
  [size & _]
  (ByteBuffer/allocate size))

(defmethod allocate [:direct :nio]
  [size & _]
  (ByteBuffer/allocateDirect size))

(defmethod allocate [:heap :netty]
  [size & _]
  (.heapBuffer allocator size))

(defmethod allocate [:direct :netty]
  [size & _]
  (.directBuffer allocator size))
