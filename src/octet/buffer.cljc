;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;
;; * Redistributions of source code must retain the above copyright notice, this
;;   list of conditions and the following disclaimer.
;;
;; * Redistributions in binary form must reproduce the above copyright notice,
;;   this list of conditions and the following disclaimer in the documentation
;;   and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
;; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
;; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns octet.buffer
  "Buffer abstractions.

  This includes a common, plataform agnostic abstractions defined
  with clojure protocols for treat with different bytebuffers
  implementations.

  Also inclues a polymorphic methods for allocate new bytebuffers
  with support for nio bytebuffers, netty41 bytebuffers and
  es6 typed arrays (from javascript environments)."
  #?(:clj
     (:import java.nio.Buffer
              java.nio.ByteBuffer
              java.nio.ByteOrder
              io.netty.buffer.ByteBuf
              io.netty.buffer.ByteBufAllocator)))

(def ^:dynamic *byte-order*
  "Defines the default byte order used for the write and read
  operations on the byte buffers."
  :big-endian)

;; --- Protocols

(defprotocol IBufferShort
  (read-short [_ pos] "Read short integer (16 bits) from buffer.")
  (write-short [_ pos value] "Write a short integer to the buffer.")
  (read-ushort [_ pos] "Read unsigned short integer (16 bits) from buffer.")
  (write-ushort [_ pos value] "Write a unsigned short integer to the buffer."))

(defprotocol IBufferInt
  (read-int [_ pos] "Read an integer (32 bits) from buffer.")
  (write-int [_ pos value] "Write an integer to the buffer.")
  (read-uint [_ pos] "Read an unsigned integer (32 bits) from buffer.")
  (write-uint [_ pos value] "Write an unsigned integer to the buffer."))

(defprotocol IBufferLong
  (read-long [_ pos] "Read an long (64 bits) from buffer.")
  (write-long [_ pos value] "Write a long to the buffer.")
  (read-ulong [_ pos] "Read an unsigned long (64 bits) from buffer.")
  (write-ulong [_ pos value] "Write an unsigned long to the buffer."))

(defprotocol IBufferFloat
  (read-float [_ pos] "Read an float (32 bits) from buffer.")
  (write-float [_ pos value] "Write a float to the buffer."))

(defprotocol IBufferDouble
  (read-double [_ pos] "Read an double (64 bits) from buffer.")
  (write-double [_ pos value] "Write a double to the buffer."))

(defprotocol IBufferByte
  (read-byte [_ pos] "Read one byte from buffer.")
  (write-byte [_ pos value] "Write one byte to the buffer.")
  (read-ubyte [_ pos] "Read one unsigned byte from buffer.")
  (write-ubyte [_ pos value] "Write one unsigned byte to the buffer."))

(defprotocol IBufferBytes
  (read-bytes [_ pos size] "Read a byte array.")
  (write-bytes [_ pos size data] "Write byte array."))

(defprotocol IBufferLimit
  (get-capacity [_] "Get the read/write capacity in bytes."))

;; ---- NIO & Netty Buffer implementations

#?(:clj
   (defn- set-current-bytebuffer-byte-order!
     [buff]
     (case *byte-order*
       :big-endian (.order ^ByteBuffer buff ByteOrder/BIG_ENDIAN)
       :little-endian (.order ^ByteBuffer buff ByteOrder/LITTLE_ENDIAN))))

#?(:clj
   (extend-type ByteBuffer
     IBufferShort
     (read-short [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.getShort buff pos))
     (write-short [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.putShort buff pos value))
     (read-ushort [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (let [val (.getShort buff pos)]
         (bit-and 0xFFFF (Integer. (int val)))))
     (write-ushort [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (let [value (.shortValue (Integer. (int value)))]
         (.putShort buff pos value)))

     IBufferInt
     (read-int [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.getInt buff pos))
     (write-int [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.putInt buff pos value))
     (read-uint [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (let [val (.getInt buff pos)]
         (bit-and 0xFFFFFFFF (Long. (long val)))))
     (write-uint [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (let [value (.intValue (Long. (long value)))]
         (.putInt buff pos value)))

     IBufferLong
     (read-long [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.getLong buff pos))
     (write-long [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.putLong buff pos value))
     (read-ulong [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (let [val (.getLong buff pos)
             ^bytes magnitude (-> (ByteBuffer/allocate 8) (.putLong val) .array)]
         (bigint (BigInteger. 1 magnitude))))
     (write-ulong [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (let [value (.longValue (bigint value))]
         (.putLong buff pos value)))

     IBufferFloat
     (read-float [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.getFloat buff pos))
     (write-float [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.putFloat buff pos value))

     IBufferDouble
     (read-double [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.getDouble buff pos))
     (write-double [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.putDouble buff pos value))

     IBufferByte
     (read-byte [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (.get buff ^Long pos))
     (write-byte [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (.put buff pos value))
     (read-ubyte [buff pos]
       (set-current-bytebuffer-byte-order! buff)
       (let [val (.get ^Long buff pos)]
         (bit-and 0xFF (short val))))
     (write-ubyte [buff pos value]
       (set-current-bytebuffer-byte-order! buff)
       (let [value (.byteValue (Short. (short value)))]
         (.put buff pos value)))

     IBufferBytes
     (read-bytes [buff pos size]
       (let [tmpbuf (byte-array size)
             oldpos (.position buff)]
         (.position ^Buffer buff ^Long pos)
         (.get buff tmpbuf)
         (.position ^Buffer buff oldpos)
         tmpbuf))
     (write-bytes [buff pos size data]
       (let [oldpos (.position ^Buffer buff)]
         (.position ^Buffer buff ^Long pos)
         (.put buff data 0 size)
         (.position ^Buffer buff oldpos)))

     IBufferLimit
     (get-capacity [buff]
       (.limit buff))))

#?(:clj
   (extend-type ByteBuf
     IBufferShort
     (read-short [buff pos]
       (if (= *byte-order* :little-endian)
         (.getShortLE buff pos)
         (.getShort buff pos)))
     (write-short [buff pos value]
       (if (= *byte-order* :little-endian)
         (.setShortLE buff pos value)
         (.setShort buff pos value)))
     (read-ushort [buff pos]
       (let [val (read-short buff pos)]
         (bit-and 0xFFFF (Integer. (int val)))))
     (write-ushort [buff pos value]
       (let [value (.shortValue (Integer. (int value)))]
         (write-short buff pos value)))

     IBufferInt
     (read-int [buff pos]
       (if (= *byte-order* :little-endian)
         (.getIntLE buff pos)
         (.getInt buff pos)))
     (write-int [buff pos value]
       (if (= *byte-order* :little-endian)
         (.setIntLE buff pos value)
         (.setInt buff pos value)))
     (read-uint [buff pos]
       (let [val (read-int buff pos)]
         (bit-and 0xFFFFFFFF (Long. (long val)))))
     (write-uint [buff pos value]
       (let [value (.intValue (Long. (long value)))]
         (write-int buff pos value)))

     IBufferLong
     (read-long [buff pos]
       (if (= *byte-order* :little-endian)
         (.getLongLE buff pos)
         (.getLong buff pos)))
     (write-long [buff pos value]
       (if (= *byte-order* :little-endian)
         (.setLongLE buff pos value)
         (.setLong buff pos value)))
     (read-ulong [buff pos]
       (let [val (read-long buff pos)
             ^bytes magnitude (-> (ByteBuffer/allocate 8) (.putLong val) .array)]
         (bigint (BigInteger. 1 magnitude))))
     (write-ulong [buff pos value]
       (let [value (.longValue (bigint value))]
         (write-long buff pos value)))

     IBufferFloat
     (read-float [buff pos]
       (if (= *byte-order* :little-endian)
         (.getFloatLE buff pos)
         (.getFloat buff pos)))
     (write-float [buff pos value]
       (if (= *byte-order* :little-endian)
         (.setFloatLE buff pos value)
         (.setFloat buff pos value)))

     IBufferDouble
     (read-double [buff pos]
       (if (= *byte-order* :little-endian)
         (.getDoubleLE buff pos)
         (.getDouble buff pos)))
     (write-double [buff pos value]
       (if (= *byte-order* :little-endian)
         (.setDoubleLE buff pos value)
         (.setDouble buff pos value)))

     IBufferByte
     (read-byte [buff pos]
       (.getByte buff pos))
     (write-byte [buff pos value]
       (.setByte buff pos value))
     (read-ubyte [buff pos]
       (let [val (.getByte buff pos)]
         (bit-and 0xFF (short val))))
     (write-ubyte [buff pos value]
       (let [value (.byteValue (Short. (short value)))]
         (.setByte buff pos value)))

     IBufferBytes
     (read-bytes [buff pos size]
       (let [tmpbuf (byte-array size)]
         (.getBytes buff ^Long pos tmpbuf)
         tmpbuf))
     (write-bytes [buff pos size data]
       (.setBytes buff ^Long pos data 0 ^Long size))

     IBufferLimit
     (get-capacity [buff]
       (.capacity buff))))

;; --- A vector of buffers

(defn- stream-operation
  [opfn buff pos]
  (reduce (fn [pos buff]
            (let [buffsize (get-capacity buff)]
                (if (>= pos buffsize)
                  (- pos buffsize)
                  (reduced (opfn buff pos)))))
            pos buff))

(extend-type #?(:clj clojure.lang.IPersistentVector
                :cljs cljs.core.PersistentVector)
  IBufferShort
  (read-short [buff pos]
    (assert (every? #(satisfies? IBufferShort %) buff))
    (stream-operation read-short buff pos))
  (write-short [buff pos value]
    (assert (every? #(satisfies? IBufferShort %) buff))
    (stream-operation #(write-short %1 %2 value) buff pos))
  (read-ushort [buff pos]
    (assert (every? #(satisfies? IBufferShort %) buff))
    (stream-operation read-ushort buff pos))
  (write-ushort [buff pos value]
    (assert (every? #(satisfies? IBufferShort %) buff))
    (stream-operation #(write-ushort %1 %2 value) buff pos))

  IBufferInt
  (read-int [buff pos]
    (assert (every? #(satisfies? IBufferInt %) buff))
    (stream-operation read-int buff pos))
  (write-int [buff pos value]
    (assert (every? #(satisfies? IBufferInt %) buff))
    (stream-operation #(write-int %1 %2 value) buff pos))
  (read-uint [buff pos]
    (assert (every? #(satisfies? IBufferInt %) buff))
    (stream-operation read-uint buff pos))
  (write-uint [buff pos value]
    (assert (every? #(satisfies? IBufferInt %) buff))
    (stream-operation #(write-uint %1 %2 value) buff pos))

  IBufferLong
  (read-long [buff pos]
    (assert (every? #(satisfies? IBufferLong %) buff))
    (stream-operation read-long buff pos))
  (write-long [buff pos value]
    (assert (every? #(satisfies? IBufferLong %) buff))
    (stream-operation #(write-long %1 %2 value) buff pos))
  (read-ulong [buff pos]
    (assert (every? #(satisfies? IBufferLong %) buff))
    (stream-operation read-ulong buff pos))
  (write-ulong [buff pos value]
    (assert (every? #(satisfies? IBufferLong %) buff))
    (stream-operation #(write-ulong %1 %2 value) buff pos))

  IBufferFloat
  (read-float [buff pos]
    (assert (every? #(satisfies? IBufferFloat %) buff))
    (stream-operation read-float buff pos))
  (write-float [buff pos value]
    (assert (every? #(satisfies? IBufferLong %) buff))
    (stream-operation #(write-float %1 %2 value) buff pos))

  IBufferDouble
  (read-double [buff pos]
    (assert (every? #(satisfies? IBufferDouble %) buff))
    (stream-operation read-double buff pos))
  (write-double [buff pos value]
    (assert (every? #(satisfies? IBufferDouble %) buff))
    (stream-operation #(write-double %1 %2 value) buff pos))

  IBufferByte
  (read-byte [buff pos]
    (assert (every? #(satisfies? IBufferByte %) buff))
    (stream-operation read-byte buff pos))
  (write-byte [buff pos value]
    (assert (every? #(satisfies? IBufferByte %) buff))
    (stream-operation #(write-byte %1 %2 value) buff pos))
  (read-ubyte [buff pos]
    (assert (every? #(satisfies? IBufferByte %) buff))
    (stream-operation read-ubyte buff pos))
  (write-ubyte [buff pos value]
    (assert (every? #(satisfies? IBufferByte %) buff))
    (stream-operation #(write-ubyte %1 %2 value) buff pos))

  IBufferBytes
  (read-bytes [buff pos size]
    (assert (every? #(satisfies? IBufferBytes %) buff))
    (stream-operation #(read-bytes %1 %2 size) buff pos))
  (write-bytes [buff pos size data]
    (assert (every? #(satisfies? IBufferByte %) buff))
    (stream-operation #(write-bytes %1 %2 size data) buff pos))

  IBufferLimit
  (get-capacity [buff]
    (reduce #(+ %1 (get-capacity %2)) 0 buff)))

;; --- ES6 Typed Arrays

#?(:cljs
   (extend-type js/DataView
     IBufferShort
     (read-short [buff pos]
       (.getInt16 buff pos (= *byte-order* :little-endian)))
     (write-short [buff pos value]
       (.setInt16 buff pos value (= *byte-order* :little-endian)))
     (read-ushort [buff pos]
       (.getUint16 buff pos (= *byte-order* :little-endian)))
     (write-ushort [buff pos value]
       (.setUint16 buff pos value (= *byte-order* :little-endian)))

     IBufferInt
     (read-int [buff pos]
       (.getInt32 buff pos (= *byte-order* :little-endian)))
     (write-int [buff pos value]
       (.setInt32 buff pos value (= *byte-order* :little-endian)))
     (read-uint [buff pos]
       (.getUint32 buff pos (= *byte-order* :little-endian)))
     (write-uint [buff pos value]
       (.setUint32 buff pos value (= *byte-order* :little-endian)))

     IBufferFloat
     (read-float [buff pos]
       (.getFloat32 buff pos (= *byte-order* :little-endian)))
     (write-float [buff pos value]
       (.setFloat32 buff pos value (= *byte-order* :little-endian)))

     IBufferDouble
     (read-double [buff pos]
       (.getFloat64 buff pos (= *byte-order* :little-endian)))
     (write-double [buff pos value]
       (.setFloat64 buff pos value (= *byte-order* :little-endian)))

     IBufferByte
     (read-byte [buff pos]
       (.getInt8 buff pos))
     (write-byte [buff pos value]
       (.setInt8 buff pos value))
     (read-ubyte [buff pos]
       (.getUint8 buff pos))
     (write-ubyte [buff pos value]
       (.setUint8 buff pos value))

     IBufferBytes
     (read-bytes [buff pos size]
       (let [offset (.-byteOffset buff)
             buffer (.-buffer buff)]
         (js/Int8Array. buffer (+ offset pos) size)))
     (write-bytes [buff pos size data]
       (doseq [i (range (.-length data))]
         (.setInt8 buff (+ pos i) (aget data i))))

     IBufferLimit
     (get-capacity [buff]
       (.-byteLength buff))))

;; --- Public Api

#?(:clj
   (def ^:private allocator (ByteBufAllocator/DEFAULT)))

(defmulti allocate
  "Polymorphic function for allocate new bytebuffers.

  It allows allocate using different implementation
  of bytebuffers and different types of them.

  As example, you may want allocate nio direct buffer of 4 bytes:

      (allocate 4 {:type :direct :impl :nio})

  The supported options pairs are:

  - type: `:heap`, impl: `:nio` (default)
  - type: `:direct`, impl: `:nio`
  - type: `:heap`, impl: `:netty`
  - type: `:direct`, impl: `:netty`
  - impl: `:es6` (clojurescript) (default)

  This function is defined as multimethod and you can
  extend it with your own bytebuffer implementations
  if you want or need it."
  (fn [size & [{:keys [type impl] :or {type :heap
                                       impl #?(:clj :nio :cljs :es6)}}]]
    [type impl]))

#?(:clj
   (defmethod allocate [:heap :nio]
     [size & _]
     (ByteBuffer/allocate size)))

#?(:clj
   (defmethod allocate [:direct :nio]
     [size & _]
     (ByteBuffer/allocateDirect size)))

#?(:clj
   (defmethod allocate [:heap :netty]
     [size & _]
     (.heapBuffer allocator size)))

#?(:clj
   (defmethod allocate [:direct :netty]
     [size & _]
     (.directBuffer allocator size)))

#?(:cljs
   (defmethod allocate [:heap :es6]
     [size & _]
     (let [bf (js/ArrayBuffer. size)]
       (js/DataView. bf))))
