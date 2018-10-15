(defproject aintegrant "0.1.0-SNAPSHOT"
  :description "Aintegrant ain't Integrant, it's Async Integrant!"
  :url "https://github.com/athos/aintegrant"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[integrant "0.6.3"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.339"]]}})
