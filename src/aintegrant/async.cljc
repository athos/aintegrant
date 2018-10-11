(ns aintegrant.async
  #?(:clj (:import [java.util.concurrent CompletableFuture]
                   [java.util.function BiConsumer])))

(defprotocol AsyncHandler
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

(defn default-async-handler []
  #?(:clj
     (reify AsyncHandler
       (-exec [this f]
         (let [fut (promise)
               thunk (fn []
                       (f (fn [ret] (.complete ^CompletableFuture @fut ret))
                          (fn [err] (.completeExceptionally ^CompletableFuture @fut err))))]
           (deliver fut (CompletableFuture/runAsync thunk))
           @fut)))
     :cljs
     (when (exists? js/Promise)
       (reify AsyncHandler
         (-exec [this f]
           (js/Promise. f))))))

(def ^:private async-handler-impl
  (atom (default-async-handler)))

(defn async-handler []
  @async-handler-impl)

(defn set-async-handler! [handler]
  (assert (satisfies? AsyncHandler handler))
  (reset! async-handler-impl handler))

(defn exec [f]
  (-exec (async-handler) f))

(defn then [task resolve reject]
  (-then task resolve reject))
