(ns aintegrant.async
  (:require [aintegrant.async.core :as async]
            #?(:clj  [aintegrant.async.future :as future]
               :cljs [aintegrant.async.promise :as promise])))

(defn default-async-executor []
  #?(:clj (future/future-async-executor)
     :cljs (promise/promise-async-executor)))

(def ^:private async-executor-impl
  (atom (default-async-executor)))

(defn async-executor []
  @async-executor-impl)

(defn set-async-executor! [executor]
  (assert (satisfies? async/AsyncExecutor executor))
  (reset! async-executor-impl executor))

(defn exec [f]
  (async/-exec (async-executor) f))

(defn then [task resolve reject]
  (async/-then task resolve reject))
