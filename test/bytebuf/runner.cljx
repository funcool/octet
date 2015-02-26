#+cljs
(ns bytebuf.runner
  (:require [cljs.test :refer-macros [run-tests]]
            [bytebuf.core-tests]
            [cljs.nodejs :as nodejs]))

#+cljs
(nodejs/enable-util-print!)

#+cljs
(defn runner
  []
  (if (cljs.test/successful? (run-tests 'bytebuf.core-tests))
    0
    1))

#+cljs
(set! *main-cli-fn* runner)
