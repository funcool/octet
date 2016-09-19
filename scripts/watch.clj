(require '[cljs.build.api :as b])

(b/watch (b/inputs "test" "src")
  {:main 'octet.tests.core
   :target :nodejs
   :output-to "out/tests.js"
   :output-dir "out"
   :optimizations :none
   :pretty-print true
   :verbose true})
