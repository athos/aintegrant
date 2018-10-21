(ns aintegrant.async.sync
  "This implementation of async executor is just for development or verifying
  the behaviors of the library; In fact, it will never execute tasks asynchronously
  at all. So, DO NOT use it for production."
  (:require [aintegrant.async.core :as async]))

(defrecord SyncTask [state]
  async/AsyncTask
  (-then [this resolve reject]
    (let [{:keys [status result]} @state]
      (try
        (if (= status :resolved)
          (resolve result)
          (reject result))
        (catch #?(:clj Throwable :cljs :default) t
          (reject t))))
    this))

(defn sync-executor []
  (reify async/AsyncExecutor
    (-exec [this f]
      (let [task (->SyncTask (atom {}))]
        (try
          (f (fn [ret] (swap! (:state task) assoc :status :resolved :result ret))
             (fn [err] (swap! (:state task) assoc :status :rejected :result err)))
          (catch #?(:clj Throwable :cljs :default) t
            (swap! (:state task) assoc :status :rejected :result t)))
        task))))
