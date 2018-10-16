(ns aintegrant.async.promise
  (:require [aintegrant.async.core :as async]))

(def promise-async-executor
  (let [executor (when (exists? js/Promise)
                   (reify async/AsyncExecutor
                     (-exec [this f]
                       (js/Promise. f))))]
    (fn [] executor)))

(when (exists? js/Promise)
  (extend-protocol async/AsyncTask
    js/Promise
    (-then [this resolve reject]
      (.then this resolve reject))))
