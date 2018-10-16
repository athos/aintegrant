(ns aintegrant.async.future
  (:require [aintegrant.async.core :as async])
  (:import [java.util.concurrent CompletableFuture]
           [java.util.function BiConsumer]))

(defn future-async-executor []
  (reify
    async/AsyncExecutor
    (-exec [this f]
      (let [fut (promise)
            thunk (fn []
                    (f (fn [ret] (.complete ^CompletableFuture @fut ret))
                       (fn [err] (.completeExceptionally ^CompletableFuture @fut err))))]
        (deliver fut (CompletableFuture/runAsync thunk))
        @fut))))

(extend-protocol async/AsyncTask
  CompletableFuture
  (-then [this resolve reject]
    (let [consumer (reify BiConsumer
                     (accept [this ret err]
                       (if err
                         (reject err)
                         (resolve ret))))]
      (.whenCompleteAsync this consumer))))
