(defproject funcool/octet "0.2.0-SNAPSHOT"
  :description "A clojure(script) library for work with binary data."
  :url "https://github.com/funcool/octet"
  :license {:name "BSD (2 Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}

  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [io.netty/netty-buffer "4.1.0.Beta7"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store|user.clj"]

  :codeina {:sources ["src"]
            :reader :clojurescript
            :exclude [octet.spec.basic
                      octet.spec.string
                      octet.spec.collections]
            :target "doc/dist/latest/api"
            :src-uri "http://github.com/funcool/beicon/blob/master/"
            :src-uri-prefix "#L"}

  :plugins [[funcool/codeina "0.3.0"]])

