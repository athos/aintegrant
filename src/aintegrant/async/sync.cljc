(ns aintegrant.async.sync
  "This implementation of async executor is just for development or verifying
  the behaviors of the library; In fact, it will never execute tasks asynchronously
  at all. So, DO NOT use it for production."
  (:require [aintegrant.async.core :as async]))

(defrecord SyncTask [state]
  async/AsyncTask
  (-then [this resolve reject]
    (let [{:keys [status result]} @state]
      (if (= status :resolved)
        (resolve result)
        (reject result)))
    this))

(defn sync-async-executor []
  (reify async/AsyncExecutor
    (-exec [this f]
      (let [task (->SyncTask (atom {}))]
        (f (fn [ret] (swap! (:state task) assoc :status :resolved :result ret))
           (fn [err] (swap! (:state task) assoc :status :rejected :result err)))
        task))))
