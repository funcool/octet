(ns bytebuf.spec
  (:refer-clojure :exclude [type read])
  (:require [bytebuf.types :as types]
            [bytebuf.proto :as proto :refer [IStaticSize]]
            [bytebuf.buffer :as buffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstraction definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defprotocol ISpec
;;   (type [_] "Get the type of spec."))

(defprotocol IReadableSpec
  (read [_ buff start] "Read all data from buffer."))

(defprotocol IWritableSpec
  (write [_ buff start data] "Read all data from buffer."))

;; (defprotocol IAssocReadableSpec
;;   (read-field [_ buff field] "Read one concrete field from buffer."))

;; (defprotocol IAssocWritableSpec
;;   (write-field [_ buff field data] "Read one concrete field from buffer."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn associative-static-spec
  [data dict types]
  (reify
    clojure.lang.Counted
    (count [_]
      (count types))

    IStaticSize
    (size [_]
      (reduce #(+ %1 (types/size %2)) 0 types))

    IReadableSpec
    (read [_ buff offset]
      (loop [index offset result {} pairs data]
        (if-let [[fieldname type] (first pairs)]
          (let [[readeddata readedbytes] (types/read type buff index)]
            (recur (+ index readedbytes)
                   (assoc result fieldname readeddata)
                   (rest pairs)))
          [(- index offset) result])))

    IWritableSpec
    (write [_ buff offset data']
      (let [written (reduce (fn [index [fieldname type]]
                              (let [value (get data' fieldname nil)
                                    written (types/write type buff index value)]
                                (+ index written)))
                            offset data)]
        (- written offset)))))

    ;; IAssocReadableSpec
    ;; (read-field [_ buff field]
    ;;   (let [ftype (get dict field)
    ;;         startpos (reduce (fn [acc [key val]]
    ;;                            (if (= key field)
    ;;                              (reduced acc)
    ;;                              (+ acc (types/size val))))
    ;;                          0 data)]
    ;;     ;; Set the buffer position
    ;;     (buffer/seek buff startpos)
    ;;     (types/read ftype buff)))))

(defn associative-dynamic-spec
  [])

(defn associative-spec
  [params]
  {:pre [(even? (count params))]}
  (let [data (mapv vec (partition 2 params))
        dict (into {} data)
        types (map second data)]
    (if (every? #(satisfies? types/IStaticType %) types)
      (associative-static-spec data dict types)
      (associative-dynamic-spec data dict types))))

(defn spec
  "Create a new spec instance."
  [& params]
  (associative-spec params))
