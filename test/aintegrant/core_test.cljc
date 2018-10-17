(ns aintegrant.core-test
  (:require [aintegrant.async.sync :as sync]
            [aintegrant.async :as async]
            [aintegrant.core :as ag]
            [clojure.test :as t :refer [deftest is testing]]
            [integrant.core :as ig]))


(t/use-fixtures :once
  (fn [f]
    (ag/set-async-executor! (sync/sync-async-executor))
    (f)
    (ag/set-async-executor! (async/default-async-executor))))

(def log (atom []))

(defmethod ig/init-key :default [k v]
  (swap! log conj [:init k v])
  [v])

(defmethod ag/init-key ::x [k v callback]
  (swap! log conj [:init k v])
  (callback :x))

(defmethod ag/init-key ::error-init1 [_ _ _]
  (throw (ex-info "Testing" {:reason ::test})))

(defmethod ag/init-key ::error-init2 [_ _ callback]
  (callback (ex-info "Testing" {:reason ::test}) nil))

(defmethod ag/init-key ::k [_ v callback] (callback v))
(defmethod ag/init-key ::n [_ v callback] (callback (inc v)))

(defmethod ig/halt-key! :default [k v]
  (swap! log conj [:halt k v]))

(defmethod ag/halt-key! ::error-halt1 [_ _ callback]
  (throw (ex-info "Testing" {:reason ::test})))

(defmethod ag/halt-key! ::error-halt2 [_ _ callback]
  (callback (ex-info "Testing" {:reason ::test}) nil))

(defmethod ig/resume-key :default [k cfg cfg' sys]
  (swap! log conj [:resume k cfg cfg' sys])
  [cfg])

(defmethod ag/resume-key ::x [k cfg cfg' sys callback]
  (swap! log conj [:resume k cfg cfg' sys])
  (callback :rx))

(defmethod ig/suspend-key! :default [k v]
  (swap! log conj [:suspend k v]))

(derive ::p ::pp)
(derive ::pp ::ppp)

(deftest init-test
  (testing "without keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), ::b 1}
             (fn [err m]
               (is (nil? err))
               (is (= m {::a [[1]], ::b [1]}))
               (is (= @log [[:init ::b 1]
                            [:init ::a [1]]])))))

  (testing "with keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), ::b 1, ::c 2} [::a]
             (fn [err m]
               (is (nil? err))
               (is (= m {::a [[1]], ::b [1]}))
               (is (= @log [[:init ::b 1]
                            [:init ::a [1]]])))))

  (testing "with inherited keys"
    (reset! log [])
    (ag/init {::p (ig/ref ::a), ::a 1} [::pp]
             (fn [err m]
               (is (nil? err))
               (is (= m {::p [[1]], ::a [1]}))
               (is (= @log [[:init ::a 1]
                            [:init ::p [1]]])))))

  (testing "large config"
    (ag/init {:a/a1 {} :a/a2 {:_ (ig/ref :a/a1)}
              :a/a3 {} :a/a4 {} :a/a5 {}
              :a/a6 {} :a/a7 {} :a/a8 {}
              :a/a9 {} :a/a10 {}}
             (fn [err m]
               (is (nil? err))
               (is (= m
                      {:a/a1 [{}] :a/a2 [{:_ [{}]}]
                       :a/a3 [{}] :a/a4 [{}] :a/a5 [{}]
                       :a/a6 [{}] :a/a7 [{}] :a/a8 [{}]
                       :a/a9 [{}] :a/a10 [{}]}))))))

(deftest halt-test
  (testing "without keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), ::b 1}
             (fn [err m]
               (is (nil? err))
               (ag/halt! m
                         (fn [err]
                           (is (nil? err))
                           (is (= @log [[:init ::b 1]
                                        [:init ::a [1]]
                                        [:halt ::a [[1]]]
                                        [:halt ::b [1]]])))))))

  (testing "with keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), ::b (ig/ref ::c), ::c 1}
             (fn [err m]
               (is (nil? err))
               (ag/halt! m [::a]
                         (fn [err]
                           (is (nil? err))
                           (is (= @log [[:init ::c 1]
                                        [:init ::b [1]]
                                        [:init ::a [[1]]]
                                        [:halt ::a [[[1]]]]]))
                           (reset! log [])
                           (ag/halt! m [::c]
                                     (fn [err]
                                       (is (nil? err))
                                       (is (= @log [[:halt ::a [[[1]]]]
                                                    [:halt ::b [[1]]]
                                                    [:halt ::c [1]]])))))))))

  (testing "with partial system"
    (reset! log [])
    (ag/init {::a 1, ::b (ig/ref ::a)} [::a]
             (fn [err m]
               (is (nil? err))
               (ag/halt! m
                         (fn [err]
                           (is (nil? err))
                           (is (= @log [[:init ::a 1]
                                        [:halt ::a [1]]])))))))

  (testing "with inherited keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::p), ::p 1} [::a]
             (fn [err m]
               (is (nil? err))
               (ag/halt! m [::pp]
                         (fn [err]
                           (is (nil? err))
                           (is (= @log [[:init ::p 1]
                                        [:init ::a [1]]
                                        [:halt ::a [[1]]]
                                        [:halt ::p [1]]])))))))

  (testing "with composite keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), [::x ::b] 1}
             (fn [err m]
               (is (nil? err))
               (ag/halt! m
                         (fn [err]
                           (is (nil? err))
                           (is (= @log [[:init [::x ::b] 1]
                                        [:init ::a :x]
                                        [:halt ::a [:x]]
                                        [:halt [::x ::b] :x]]))))))))
