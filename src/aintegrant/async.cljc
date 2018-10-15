(ns aintegrant.async
  #?(:clj (:import [java.util.concurrent CompletableFuture]
                   [java.util.function BiConsumer])))

(defprotocol AsyncExecutor
  (-exec [this f]))

(defprotocol AsyncTask
  (-then [this resolve reject]))

#?(:clj
   (extend-protocol AsyncTask
     CompletableFuture
     (-then [this resolve reject]
       (let [consumer (reify BiConsumer
                        (accept [this ret err]
                          (if err
                            (reject err)
                            (resolve ret))))]
         (.whenCompleteAsync this consumer)))))

#?(:cljs
   (when (exists? js/Promise)
     (extend-protocol AsyncTask
       js/Promise
       (-then [this resolve reject]
         (.then this resolve reject)))))

(defn default-async-executor []
  #?(:clj
     (reify AsyncExecutor
       (-exec [this f]
         (let [fut (promise)
               thunk (fn []
                       (f (fn [ret] (.complete ^CompletableFuture @fut ret))
                          (fn [err] (.completeExceptionally ^CompletableFuture @fut err))))]
           (deliver fut (CompletableFuture/runAsync thunk))
           @fut)))
     :cljs
     (when (exists? js/Promise)
       (reify AsyncExecutor
         (-exec [this f]
           (js/Promise. f))))))

(def ^:private async-executor-impl
  (atom (default-async-executor)))

(defn async-executor []
  @async-executor-impl)

(defn set-async-executor! [executor]
  (assert (satisfies? AsyncExecutor executor))
  (reset! async-executor-impl executor))

(defn exec [f]
  (-exec (async-executor) f))

(defn then [task resolve reject]
  (-then task resolve reject))
