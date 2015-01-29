(defproject github-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [tentacles "0.3.0"]
                 [criterium "0.4.3"]
                 [http-kit "2.1.16"]
                 [com.climate/claypoole "0.4.0"]]
  :main ^:skip-aot github-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
