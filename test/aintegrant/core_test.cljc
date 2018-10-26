(ns aintegrant.core-test
  (:require [aintegrant.async :as async]
            [aintegrant.async.serial :as serial]
            [aintegrant.core :as ag]
            [clojure.test :as t :refer [deftest is testing]]
            [integrant.core :as ig]))


(t/use-fixtures :once
  (fn [f]
    (ag/set-async-executor! (serial/serial-async-executor))
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
  (callback (ex-info "Testing" {:reason ::test})))

(defmethod ag/resume-key :default [k cfg cfg' sys callback]
  (swap! log conj [:resume k cfg cfg' sys])
  (callback [cfg]))

(defmethod ag/resume-key ::x [k cfg cfg' sys callback]
  (swap! log conj [:resume k cfg cfg' sys])
  (callback :rx))

(defmethod ag/suspend-key! :default [k v callback]
  (swap! log conj [:suspend k v])
  (callback))

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

  (testing "with composite keys"
    (reset! log [])
    (ag/init {::a (ig/ref ::b), [::x ::b] 1}
             (fn [err m]
               (is (nil? err))
               (is (= m {::a [:x], [::x ::b] :x}))
               (is (= @log [[:init [::x ::b] 1]
                            [:init ::a :x]])))))

  (testing "with composite refs"
    (reset! log [])
    (ag/init {::a (ig/ref [::b ::c]), [::b ::c ::e] 1, [::b ::d] 2}
             (fn [err m]
               (is (nil? err))
               (is (= m {::a [[1]], [::b ::c ::e] [1], [::b ::d] [2]}))
               (is (or (= @log [[:init [::b ::c ::e] 1]
                                [:init ::a [1]]
                                [:init [::b ::d] 2]])
                       (= @log [[:init [::b ::d] 2]
                                [:init [::b ::c ::e] 1]
                                [:init ::a [1]]]))))))

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

(deftest suspend-resume-test
  (testing "same configuration"
    (reset! log [])
    (let [c  {::a (ig/ref ::b), ::b 1}]
      (ag/init c
               (fn [err1 m]
                 (is (nil? err1))
                 (ag/suspend! m
                              (fn [err2]
                                (is (nil? err2))
                                (ag/resume c m
                                           (fn [err3 m']
                                             (is (nil? err3))
                                             (is (= @log [[:init ::b 1]
                                                          [:init ::a [1]]
                                                          [:suspend ::a [[1]]]
                                                          [:suspend ::b [1]]
                                                          [:resume ::b 1 1 [1]]
                                                          [:resume ::a [1] [1] [[1]]]]))))))))))

  (testing "missing keys"
    (reset! log [])
    (let [c  {::a (ig/ref ::b), ::b 1}]
      (ag/init c
               (fn [err1 m]
                 (is (nil? err1))
                 (ag/suspend! m
                              (fn [err2]
                                (is (nil? err2))
                                (ag/resume (dissoc c ::a) m
                                           (fn [err3 m']
                                             (is (nil? err3))
                                             (is (= @log [[:init ::b 1]
                                                          [:init ::a [1]]
                                                          [:suspend ::a [[1]]]
                                                          [:suspend ::b [1]]
                                                          [:halt ::a [[1]]]
                                                          [:resume ::b 1 1 [1]]]))))))))))

  (testing "missing refs"
    (reset! log [])
    (let [c  {::a {:b (ig/ref ::b)}, ::b 1}]
      (ag/init c
               (fn [err1 m]
                 (is (nil? err1))
                 (ag/suspend! m
                              (fn [err2]
                                (is (nil? err2))
                                (ag/resume {::a []} m
                                           (fn [err3 m']
                                             (is (nil? err3))
                                             (is (= @log [[:init ::b 1]
                                                          [:init ::a {:b [1]}]
                                                          [:suspend ::a [{:b [1]}]]
                                                          [:suspend ::b [1]]
                                                          [:halt ::b [1]]
                                                          [:resume ::a [] {:b [1]} [{:b [1]}]]]))))))))))

  (testing "composite keys"
    (reset! log [])
    (let [c  {::a (ig/ref ::x), [::b ::x] 1}]
      (ag/init c
               (fn [err1 m]
                 (is (nil? err1))
                 (ag/suspend! m
                              (fn [err2]
                                (is (nil? err2))
                                (ag/resume c m
                                           (fn [err3 m']
                                             (is (nil? err3))
                                             (is (= @log [[:init [::b ::x] 1]
                                                          [:init ::a :x]
                                                          [:suspend ::a [:x]]
                                                          [:suspend [::b ::x] :x]
                                                          [:resume [::b ::x] 1 1 :x]
                                                          [:resume ::a :rx :x [:x]]]))))))))))

  (testing "resume key with dependencies"
    (reset! log [])
    (let [c  {::a {:b (ig/ref ::b)}, ::b 1}]
      (ag/init c [::a]
               (fn [err1 m]
                 (is (nil? err1))
                 (ag/suspend! m
                              (fn [err2]
                                (is (nil? err2))
                                (ag/resume c m [::a]
                                           (fn [err3 m']
                                             (is (nil? err3))
                                             (is (= @log
                                                    [[:init ::b 1]
                                                     [:init ::a {:b [1]}]
                                                     [:suspend ::a [{:b [1]}]]
                                                     [:suspend ::b [1]]
                                                     [:resume ::b 1 1 [1]]
                                                     [:resume ::a {:b [1]} {:b [1]} [{:b [1]}]]])))))))))))

(deftest wrapped-exception-test
  (testing "exception when building"
    (letfn [(callback-for [key]
              (fn [ex ret]
                (is (nil? ret))
                (is (some? ex))
                (is (= (#?(:clj .getMessage :cljs ex-message) ex)
                       (str "Error on key " key " when building system")))
                (is (= (ex-data ex)
                       {:reason   ::ig/build-threw-exception
                        :system   {::a [1]}
                        :function ag/init-key
                        :key      key
                        :value    [1]}))
                (let [cause (#?(:clj .getCause :cljs ex-cause) ex)]
                  (is (some? cause))
                  (is (= (#?(:clj .getMessage :cljs ex-message) cause) "Testing"))
                  (is (= (ex-data cause) {:reason ::test})))))]
      (ag/init {::a 1, ::error-init1 (ig/ref ::a)} (callback-for ::error-init1))
      (ag/init {::a 1, ::error-init2 (ig/ref ::a)} (callback-for ::error-init2))))

  (testing "exception when running"
    (letfn [(callback-for [key]
              (fn [ex system]
                (is (nil? ex))
                (ag/halt! system
                          (fn [ex]
                            (is (some? ex))
                            (is (= (#?(:clj .getMessage :cljs ex-message) ex)
                                   (str "Error on key " key " when running system")))
                            (is (= (ex-data ex)
                                   {:reason         ::ig/run-threw-exception
                                    :system         {::a [1], key [[1]], ::b [[[1]]], ::c [[[[1]]]]}
                                    :completed-keys '(::c ::b)
                                    :remaining-keys '(::a)
                                    :function       ag/halt-key!
                                    :key            key
                                    :value          [[1]]}))
                            (let [cause (#?(:clj .getCause :cljs ex-cause) ex)]
                              (is (some? cause))
                              (is (= (#?(:clj .getMessage :cljs ex-message) cause) "Testing"))
                              (is (= (ex-data cause) {:reason ::test})))))))]
      (ag/init {::a 1
                ::error-halt1 (ig/ref ::a)
                ::b (ig/ref ::error-halt1)
                ::c (ig/ref ::b)}
               (callback-for ::error-halt1))
      (ag/init {::a 1
                ::error-halt2 (ig/ref ::a)
                ::b (ig/ref ::error-halt2)
                ::c (ig/ref ::b)}
               (callback-for ::error-halt2)))))
