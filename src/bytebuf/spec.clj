(ns bytebuf.spec
  (:refer-clojure :exclude [type read])
  (:require [bytebuf.types :as types]
            [bytebuf.buffer :as buffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defprotocol ISpec
;;   (type [_] "Get the type of spec."))

(defprotocol ISpecSize
  (size [_] [_ data] "Calculate the size for this spec."))

(defprotocol IReadableSpec
  (read [_ buff] "Read all data from buffer."))

(defprotocol IWritableSpec
  (write [_ buff data] "Read all data from buffer."))

(defprotocol IAssocReadableSpec
  (read-field [_ buff field] "Read one concrete field from buffer."))

(defprotocol IAssocWritableSpec
  (write-field [_ buff field] "Read one concrete field from buffer."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn associative-static-spec
  [data dict types]
  (reify
    ISpecSize
    (size [_]
      (reduce #(+ %1 (types/size %2)) 0 types))
    (size [_ data]
      (reduce #(+ %1 (types/size %2)) 0 types))

    IReadableSpec
    (read [_ buff]
      (reduce (fn [acc [name t]]
                (assoc acc name (types/read t buff)))
              {}
              data))

    IWritableSpec
    (write [_ buff data']
      (reduce (fn [buff [fieldname type]]
                (let [value (get data' fieldname nil)]
                  (types/write type buff value)
                  buff))
              buff data))

    IAssocReadableSpec
    (read-field [_ buff field]
      (let [ftype (get dict field)
            startpos (reduce (fn [acc [key val]]
                               (if (= key field)
                                 (reduced acc)
                                 (+ acc (types/size val))))
                             0 data)]
        ;; Set the buffer position
        (buffer/seek buff startpos)
        (types/read ftype buff)))))

(defn associative-dynamic-spec
  [])

(defn associative-spec
  [params]
  {:pre [(even? (count params))]}
  (let [data (partition 2 params)
        dict (into {} data)
        types (map second data)]
    (if (every? #(satisfies? types/IStaticType %) types)
      (associative-static-spec data dict types)
      (associative-dynamic-spec data dict types))))

(defn spec
  "Create a new spec instance."
  [& params]
  (associative-spec params))
