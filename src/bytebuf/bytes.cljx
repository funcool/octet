(ns bytebuf.bytes
  #+clj
  (:import java.util.Arrays
           java.security.SecureRandom))

#+clj
(defn random-bytes
  "Generate a byte array of scpecified length with random
  bytes taken from secure random number generator.
  This method should be used for generate a random
  iv/salt or arbitrary length."
  ([^long numbytes]
   (random-bytes numbytes (SecureRandom.)))
  ([^long numbytes ^SecureRandom sr]
   (let [buffer (byte-array numbytes)]
     (.nextBytes sr buffer)
     buffer)))

#+clj
(defn slice
  "Given a byte array, get a copy of it. If offset
  and limit is provided, a slice will be returned."
  [^bytes input ^long offset ^long limit]
  (Arrays/copyOfRange input offset limit))

#+clj
(defn equals?
  "Check if two byte arrays are equals."
  [^bytes a ^bytes b]
  (Arrays/equals a b))

#+clj
(defn zeropad!
  "Add zero byte padding to the given byte array
  to the remaining bytes after specified data length."
  [^bytes input ^long datalength]
  (Arrays/fill input datalength (count input) (byte 0)))

(defn zeropad-count
  "Given a byte array, returns a number of bytes
  allocated with zero padding (zero byte)."
  [input]
  (let [mark (byte 0)]
    (reduce (fn [sum index]
              (let [value (aget input index)]
                (if (= value mark)
                  (inc sum)
                  (reduced sum))))
            0
            (reverse (range (count input))))))
