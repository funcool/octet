(ns bytebuf.buffer
  "Buffer abstractions."
  #+clj
  (:import java.nio.ByteBuffer
           io.netty.buffer.ByteBuf
           io.netty.buffer.ByteBufAllocator))

(defprotocol IBufferShort
  (read-short [_ pos] "Read short integer (16 bits) from buffer.")
  (write-short [_ pos value] "Write a short integer to the buffer."))

(defprotocol IBufferInt
  (read-int [_ pos] "Read an integer (32 bits) from buffer.")
  (write-int [_ pos value] "Write an integer to the buffer."))

(defprotocol IBufferLong
  (read-long [_ pos] "Read an long (64 bits) from buffer.")
  (write-long [_ pos value] "Write a long to the buffer."))

(defprotocol IBufferFloat
  (read-float [_ pos] "Read an float (32 bits) from buffer.")
  (write-float [_ pos value] "Write a float to the buffer."))

(defprotocol IBufferDouble
  (read-double [_ pos] "Read an double (64 bits) from buffer.")
  (write-double [_ pos value] "Write a double to the buffer."))

(defprotocol IBufferByte
  (read-byte [_ pos] "Read one byte from buffer.")
  (write-byte [_ pos value] "Write one byte to the buffer."))

(defprotocol IBufferBytes
  (read-bytes [_ pos size] "Read a byte array.")
  (write-bytes [_ pos size data] "Write byte array."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NIO & Netty Buffer implementations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#+clj
(extend-type ByteBuffer
  IBufferShort
  (read-short [buff pos]
    (.getShort buff pos))
  (write-short [buff pos value]
    (.putShort buff pos value))

  IBufferInt
  (read-int [buff pos]
    (.getInt buff pos))
  (write-int [buff pos value]
    (.putInt buff pos value))

  IBufferLong
  (read-long [buff pos]
    (.getLong buff pos))
  (write-long [buff pos value]
    (.putLong buff pos value))

  IBufferFloat
  (read-float [buff pos]
    (.getFloat buff pos))
  (write-float [buff pos value]
    (.putFloat buff pos value))

  IBufferDouble
  (read-double [buff pos]
    (.getDouble buff pos))
  (write-double [buff pos value]
    (.putDouble buff pos value))

  IBufferByte
  (read-byte [buff pos]
    (.get buff pos))
  (write-byte [buff pos value]
    (.put buff pos value))

  IBufferBytes
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
      (.position buff oldpos))))

#+clj
(extend-type ByteBuf
  IBufferShort
  (read-short [buff pos]
    (.getShort buff pos))
  (write-short [buff pos value]
    (.setShort buff pos value))

  IBufferInt
  (read-int [buff pos]
    (.getInt buff pos))
  (write-int [buff pos value]
    (.setInt buff pos value))

  IBufferLong
  (read-long [buff pos]
    (.getLong buff pos))
  (write-long [buff pos value]
    (.setLong buff pos value))

  IBufferFloat
  (read-float [buff pos]
    (.getFloat buff pos))
  (write-float [buff pos value]
    (.setFloat buff pos value))

  IBufferDouble
  (read-double [buff pos]
    (.getDouble buff pos))
  (write-double [buff pos value]
    (.setDouble buff pos value))

  IBufferByte
  (read-byte [buff pos]
    (.getByte buff pos))
  (write-byte [buff pos value]
    (.setByte buff pos value))

  IBufferBytes
  (read-bytes [buff pos size]
    (let [tmpbuf (byte-array size)]
      (.getBytes buff pos tmpbuf)
      tmpbuf))
  (write-bytes [buff pos size data]
    (.setBytes buff pos data 0 size)))

#+cljs
(extend-type js/DataView
  IBufferShort
  (read-short [buff pos]
    (.getInt16 buff pos))
  (write-short [buff pos value]
    (.setInt16 buff pos value))

  IBufferInt
  (read-int [buff pos]
    (.getInt32 buff pos))
  (write-int [buff pos value]
    (.setInt32 buff pos value))

  IBufferFloat
  (read-float [buff pos]
    (.getFloat32 buff pos))
  (write-float [buff pos value]
    (.setFloat32 buff pos value))

  IBufferDouble
  (read-double [buff pos]
    (.getFloat64 buff pos))
  (write-double [buff pos value]
    (.setFloat64 buff pos value))

  IBufferByte
  (read-byte [buff pos]
    (.getInt8 buff pos))
  (write-byte [buff pos value]
    (.setInt8 buff pos value)))

  ;; IBufferBytes
  ;; (read-bytes [buff pos size]
  ;;   (let [offset (.-byteOffset buff)
  ;;         buffer (.-buffer buff)]
  ;;     (js/Int8Array. buffer (+ offset pos) size)))
  ;; (write-bytes [buff pos size data]
  ;;   (doseq [i (range (.-length data))]
  ;;     (.setInt8 buff (+ pos i) (aget data i)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public Api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#+clj
(def ^{:private true}
  allocator (ByteBufAllocator/DEFAULT))

(defmulti allocate
  (fn [size & [{:keys [type impl] :or {type :heap impl #+clj :nio #+cljs :es6}}]]
    [type impl]))

#+clj
(defmethod allocate [:heap :nio]
  [size & _]
  (ByteBuffer/allocate size))

#+clj
(defmethod allocate [:direct :nio]
  [size & _]
  (ByteBuffer/allocateDirect size))

#+clj
(defmethod allocate [:heap :netty]
  [size & _]
  (.heapBuffer allocator size))

#+clj
(defmethod allocate [:direct :netty]
  [size & _]
  (.directBuffer allocator size))
