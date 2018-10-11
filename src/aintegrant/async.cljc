(ns aintegrant.async
  (:import [java.util.concurrent CompletableFuture]
           [java.util.function BiConsumer]))

(defprotocol AsyncHandler
  (-exec [this f]))

(defprotocol AsyncTask
  (-then [this resolve reject]))

(extend-protocol AsyncTask
  CompletableFuture
  (-then [this resolve reject]
    (let [consumer (reify BiConsumer
                     (accept [this ret err]
                       (if err
                         (reject err)
                         (resolve ret))))]
      (.whenCompleteAsync this consumer))))

(defn default-async-handler []
  (reify AsyncHandler
    (-exec [this f]
      (let [fut (atom nil)
            thunk (fn []
                    (f (fn [ret] (.complete ^CompletableFuture @fut ret))
                       (fn [err] (.completeExceptionally ^CompletableFuture @fut err))))]
        (reset! fut (CompletableFuture/runAsync thunk))))))

(def ^:private async-handler-impl
  (atom (default-async-handler)))

(defn async-handler []
  @async-handler-impl)

(defn set-async-handler! [handler]
  {:pre (satisfies? AsyncHandler handler)}
  (reset! async-handler-impl handler))

(defn exec [f]
  (-exec (async-handler) f))

(defn then [task resolve reject]
  (-then task resolve reject))
