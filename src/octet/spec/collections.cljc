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

(ns octet.spec.collections
  (:require [octet.buffer :as buffer]
            [octet.spec :as spec]))

(defn vector*
  "Create a arbitrary length (dynamic) array like
  typespec composition for `t` type."
  [t]
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
      (+ 4 (reduce #(+ %1 (if (satisfies? spec/ISpecDynamicSize t)
                            (spec/size* t %2)
                            (spec/size t))) 0 data)))
    spec/ISpec
    (read [_ buff pos]
      (let [nitems (buffer/read-int buff pos)
            typespec (spec/repeat nitems t)
            [readed data] (spec/read typespec buff (+ pos 4))]
        [(+ readed 4) data]))

    (write [_ buff pos input]
      (let [nitems (count input)
            typespec (spec/repeat nitems t)]
        (buffer/write-int buff pos nitems)
        (+ 4 (spec/write typespec buff (+ pos 4) input))))))
