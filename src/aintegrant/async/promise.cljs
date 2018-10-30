(ns aintegrant.async.promise
  (:require [aintegrant.async.core :as async])
  (:import goog.Promise))

(defn native-promise-executor []
  (reify async/AsyncExecutor
    (-exec [this f]
      (js/Promise. f))))

(defn closure-promise-executor []
  (reify async/AsyncExecutor
    (-exec [this f]
      (Promise. f))))

(def promise-async-executor
  (let [executor (cond (exists? js/Promise) (native-promise-executor)
                       (or (exists? js/setImmediate)
                           (exists? js/setTimeout))
                       (closure-promise-executor)
                       :else nil)]
    (fn [] executor)))

(extend-type Promise
  async/AsyncTask
  (-then [this resolve reject]
    (.then this resolve reject)))

(when (exists? js/Promise)
  (extend-type js/Promise
    async/AsyncTask
    (-then [this resolve reject]
      (.then this resolve reject))))
