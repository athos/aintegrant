(ns aintegrant.test-runner
  (:require aintegrant.core-test
            [clojure.test :as t]))

(defn exit-with [{:keys [fail error]}]
  (let [code (if (zero? (+ fail error)) 0 1)]
    #?(:clj (System/exit code)
       :cljs (assert (= code 0)))))

#?(:cljs
   (defmethod t/report [::t/default :end-run-tests] [summary]
     (exit-with summary)))

(defn -main []
  (let [summary (t/run-tests 'aintegrant.core-test)]
    #?(:clj (exit-with summary))))
