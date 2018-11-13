(ns aintegrant.test-coverage
  (:require [cloverage.coverage :as coverage]))

(defn -main []
  (coverage/run-project {:src-ns-path ["src"]
                         :test-ns-path ["test"]
                         :test-ns-regex [#"aintegrant\..*-test"]
                         :codecov? true}))
