#+cljs
(ns octet.tests.main
  (:require [cljs-testrunners.node :as node]
            [octet.tests.core]))

#+cljs
(defn -main
  []
  (node/run-tests 'octet.tests.core))

#+cljs
(set! *main-cli-fn* -main)
