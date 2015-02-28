#+cljs
(ns bytebuf.tests.main
  (:require [cljs-testrunners.node :as node]
            [bytebuf.tests.core]))

#+cljs
(defn -main
  []
  (node/run-tests 'bytebuf.tests.core))

#+cljs
(set! *main-cli-fn* -main)
