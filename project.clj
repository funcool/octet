(defproject funcool/octet "0.1.0"
  :description "A clojure(script) library for work with binary data."
  :url "https://github.com/funcool/octet"
  :license {:name "BSD (2 Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [io.netty/netty-buffer "4.1.0.Beta3"]
                 [org.clojure/clojurescript "0.0-2913"]]

  :source-paths ["output/src"]
  :test-paths ["output/test/clj"]

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store|user.clj"]
  :cljx {:builds [{:source-paths ["src"]
                   :output-path "output/src"
                   :rules :clj}
                  {:source-paths ["src"]
                   :output-path "output/src"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "output/test/clj"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "output/test/cljs"
                   :rules :cljs}]}

  :cljsbuild {:test-commands {"test" ["node" "output/tests.js"]}
              :builds [{:id "dev"
                        :source-paths ["output/test/cljs" "output/src"]
                        :notify-command ["node" "output/tests.js"]
                        :compiler {:output-to "output/tests.js"
                                   :output-dir "output/out"
                                   :static-fns true
                                   :cache-analysis false
                                   :main octet.tests.main
                                   :source-map true
                                   :optimizations :none
                                   :target :nodejs
                                   :pretty-print true}}]}
  :profiles {:dev {:plugins [[org.clojars.cemerick/cljx "0.6.0-SNAPSHOT"
                              :exclude [org.clojure/clojure]]
                             [lein-cljsbuild "1.0.4"]]
                   :dependencies [[funcool/cljs-testrunners "0.1.0-SNAPSHOT"]]}})

