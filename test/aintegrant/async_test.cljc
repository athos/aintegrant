(ns aintegrant.async-test
  (:require [aintegrant.async :as async]
            [clojure.test :refer #?(:clj [deftest is] :cljs [deftest is async])]))

#?(:clj
   (defmacro async [done & body]
     `(let [p# (promise)
            ~done #(deliver p# nil)]
        ~@body
        @p#)))

(defn should-not-be-called [_]
  (assert false "Should not be called"))

(deftest success-test
  (async done
    (-> (async/exec (fn [resolve reject] (resolve 42)))
        (async/then (fn [x]
                      (is (= 42 x))
                      (done))
                    should-not-be-called))))

(deftest fail-test
  (async done
    (-> (async/exec (fn [resolve reject] (reject (ex-info "failed" {:reason :test}))))
        (async/then should-not-be-called
                    (fn [err]
                      (is (= :test (:reason (ex-data err))))
                      (done))))))

(deftest success-success-test
  (async done
    (-> (async/exec (fn [resolve reject] (resolve 41)))
        (async/then (fn [x]
                      (-> (async/exec (fn [resolve reject] (resolve (inc x))))
                          (async/then (fn [x]
                                        (is (= 42 x))
                                        (done))
                                      should-not-be-called)))
                    should-not-be-called))))

(deftest error-test
  (async done
    (-> (async/exec (fn [resolve reject] (throw (ex-info "error" {:reason :test}))))
        (async/then should-not-be-called
                    (fn [err]
                      (is (= :test (:reason (ex-data err))))
                      (done))))))
