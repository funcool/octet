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

(ns octet.util
  (:require [clojure.string :as str :refer [join ]])
  (:import [java.util.Arrays]))

(defmacro defalias
  [sym sym2]
  `(do
     (def ~sym ~sym2)
     (alter-meta! (var ~sym) merge (dissoc (meta (var ~sym2)) :name))))

(defn assoc-ordered [a-map key val & rest]
  "assoc into an array-map, keeping insertion order. The normal clojure
  assoc function switches to hash maps on maps > size 10 and loses insertion order"
  (let [kvs (interleave (concat (keys a-map) (list key))
                        (concat (vals a-map) (list val)))
        ret (apply array-map kvs)]
    (if rest
      (if (next rest)
        (recur ret (first rest) (second rest) (nnext rest))
        (throw (IllegalArgumentException.
                 "assoc-ordered expects even number of arguments after map/vector, found odd number")))
      ret)))

;;
;; Hexdumps
;;


(defn- bytes->hex [^bytes bytes]
  "converts a byte array to a hex string"
  (let [[f & r] bytes
        fh (fn [_ b]
             (let [h (Integer/toHexString (bit-and b 0xFF))]
               (if (<= 0 b 15) (str "0" h) h)))]
    (join (reductions fh (fh 0 f) r))))

(defn- byte->ascii [byte]
  "convert a byte to 'printable' ascii where possible, otherwise ."
  (if (<= 32 (bit-and byte 0xFF) 127) (char byte) \.))

(defn- bytes->ascii [^bytes bytes]
  "returns a 16-per-line printable ascii view of the bytes"
  (->> bytes
       (map byte->ascii)
       (partition 16 16 "                ")
       (map join)))

(defn- format-hex-line [^String hex-line]
  "formats a 'line' (32 hex chars) of hex output"
  (->> hex-line
       (partition-all 4)
       (map join)
       (split-at 4)
       (map #(join " " %))
       (join "  ")))

(defn- bytes->hexdump [^bytes bytes]
  "formats a byte array to a sequence of formatted hex lines"
  (->> bytes
       bytes->hex
       (partition 32 32 (join (repeat 32 " ")))
       (map format-hex-line)))

(defn- copy-bytes [bytes offset size]
  "utility function - copy bytes, return new byte array"
  (let [size (if (nil? size) (alength bytes) size)]
    (if (and (= 0 offset) (= (alength bytes) size))
      bytes                                                 ; short circuit
      (java.util.Arrays/copyOfRange bytes
                                    offset
                                  (+ offset size)))))

(defn get-dump-bytes [x offset size]
  "utility function - return byte array from offset offset and with
  size size for nio ByteBuffer, netty ByteBuf, byte array, and String"
  (cond (and (satisfies? octet.buffer/IBufferBytes x)
             (satisfies? octet.buffer/IBufferLimit x))
        (let [size (if (nil? size) (octet.buffer/get-capacity x) size)]
          (octet.buffer/read-bytes x offset size))

        (instance? (type (byte-array 0)) x)
        (copy-bytes x offset size)

        (instance? String x)
        (copy-bytes (.getBytes x) offset size)))


; Example usage of hex-dump
;
;(hex-dump (byte-array (range 200)))
; --------------------------------------------------------------------
;|00000000: 0001 0203 0405 0607  0809 0a0b 0c0d 0e0f  ................|
;|00000010: 1011 1213 1415 1617  1819 1a1b 1c1d 1e1f  ................|
;|00000020: 2021 2223 2425 2627  2829 2a2b 2c2d 2e2f   !"#$%&'()*+,-./|
;|00000030: 3031 3233 3435 3637  3839 3a3b 3c3d 3e3f  0123456789:;<=>?|
;|00000040: 4041 4243 4445 4647  4849 4a4b 4c4d 4e4f  @ABCDEFGHIJKLMNO|
;|00000050: 5051 5253 5455 5657  5859 5a5b 5c5d 5e5f  PQRSTUVWXYZ[\]^_|
;|00000060: 6061 6263 6465 6667  6869 6a6b 6c6d 6e6f  `abcdefghijklmno|
;|00000070: 7071 7273 7475 7677  7879 7a7b 7c7d 7e7f  pqrstuvwxyz{|}~|
;|00000080: 8081 8283 8485 8687  8889 8a8b 8c8d 8e8f  ................|
;|00000090: 9091 9293 9495 9697  9899 9a9b 9c9d 9e9f  ................|
;|000000a0: a0a1 a2a3 a4a5 a6a7  a8a9 aaab acad aeaf  ................|
;|000000b0: b0b1 b2b3 b4b5 b6b7  b8b9 babb bcbd bebf  ................|
;|000000c0: c0c1 c2c3 c4c5 c6c7                       ........        |
; --------------------------------------------------------------------

(defn hex-dump
  "Create hex dump. Accepts byte array, java.nio.ByteBuffer,
  io.netty.buffer.ByteBuf, or String as first argument. Offset will
  start the dump from an offset in the byte array, size will limit
  the number of bytes dumped, and frames will print a frame around
  the dump if true. Set print to true to print the dump on stdout
  (default) or false to return it as a string. Example call:
  (hex-dump (byte-array (range 200)) :print false)"
  [x & {:keys [offset size print frame]
                   :or   {offset 0
                          print  true
                          frame true}}]
  {:pre [(not (nil? x))]}
  (let [bytes (get-dump-bytes x offset size)
        size (if (nil? size) (alength bytes) size)
        dump (bytes->hexdump bytes)
        ascii (bytes->ascii bytes)
        offs (map #(format "%08x" %)
                  (range offset (+ offset size 16) 16))
        header (str " " (join (repeat 68 "-")))
        border (if frame "|" "")
        lines (map #(str border %1 ": " %2 "  " %3 border) offs dump ascii)
        lines (if frame (concat [header] lines [header]) lines)
        result (join \newline lines)]
    (if print (println result) result)))
