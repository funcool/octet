(defproject funcool/octet "0.2.0-SNAPSHOT"
  :description "A clojure(script) library for work with binary data."
  :url "https://github.com/funcool/octet"
  :license {:name "BSD (2 Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[io.netty/netty-buffer "4.1.0.Beta4"]]

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
                                   :output-dir "output/"
                                   :source-map true
                                   :static-fns true
                                   :cache-analysis false
                                   :main octet.tests.core
                                   :optimizations :none
                                   :target :nodejs
                                   :pretty-print true}}]}

  :codeina {:output-dir "doc/api"
            :sources ["output/src"]
            :exclude [octet.spec.basic
                      octet.spec.string
                      octet.spec.collections]
            :language :clojure
            :src-uri-mapping {#"output/src" #(str "src/" % "x")}
            :src-dir-uri "http://github.com/funcool/octet/blob/master/"
            :src-linenum-anchor-prefix "L"}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/clojurescript "0.0-3126"]
                                  [funcool/cljs-testrunners "0.1.0-SNAPSHOT"]]
                   :plugins [[com.keminglabs/cljx "0.6.0"
                              :exclude [org.clojure/clojure]]
                             [funcool/codeina "0.1.0-SNAPSHOT"
                              :exclude [org.clojure/clojure]]
                             [lein-cljsbuild "1.0.4"]]}})
