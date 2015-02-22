(ns bytebuf.bytes
  (:import java.util.Arrays))

(defn slice
  "Given a byte array, get a copy of it. If offset
  and limit is provided, a slice will be returned."
  [^bytes input ^long offset ^long limit]
  (Arrays/copyOfRange input offset limit))

(defn zeropad!
  "Add zero byte padding to the given byte array
  to the remaining bytes after specified data length."
  [^bytes input ^long datalength]
  (Arrays/fill input datalength (count input) (byte 0)))

(defn zeropad-count
  "Given a byte array, returns a number of bytes
  allocated with zero padding (zero byte)."
  [^bytes input]
  (let [mark (byte 0)
        pos (reduce (fn [previndex index]
                      (let [value (aget input index)]
                        (if (= value mark)
                          index
                          (reduced previndex))))
                    (reverse (range (count input))))]
    (- (count input) pos)))
