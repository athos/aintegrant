(ns aintegrant.core
  (:refer-clojure :exclude [run!])
  (:require [aintegrant.async :as async]
            [integrant.core :as ig]))

(defmulti init-key
  {:arglists '([key value callback])}
  (fn [key value callback] (#'ig/normalize-key key)))

(defmethod init-key :default [k v callback]
  (callback (ig/init-key k v)))

(defmulti halt-key!
  {:arglists '([key value callback])}
  (fn [key value callback] (#'ig/normalize-key key)))

(defmethod halt-key! :default [k v callback]
  (ig/halt-key! k v)
  (callback))

(defmulti resume-key
  {:arglists '([key value old-value old-impl callback])}
  (fn [key value old-value old-impl callback] (#'ig/normalize-key key)))

(defmethod resume-key :default [k v _ _ callback]
  (init-key k v callback))

(defmulti suspend-key!
  {:arglists '([key value callback])}
  (fn [key value callback] (#'ig/normalize-key key)))

(defmethod suspend-key! :default [k v callback]
  (halt-key! k v callback))

(defn- wrap-run-callback [resolve reject]
  (fn
    ([] (resolve nil))
    ([err] (if err (reject err) (resolve nil)))))

(defn- try-run-action [system completed remaining f k resolve reject]
  (let [v (system k)
        reject' #(reject (#'ig/run-exception system completed remaining f k v %))]
    (try
      (f k v (wrap-run-callback resolve reject'))
      (catch #?(:clj Throwable :cljs :default) t
        (reject' t)))))

(defn- run-loop [system keys f callback]
  (letfn [(step [completed remaining]
            (if (seq remaining)
              (let [k (first remaining)]
                (-> (async/exec (partial try-run-action system completed remaining f k))
                    (async/then (fn [_]
                                  (step (cons k completed)
                                        (rest remaining)))
                                (fn [err] (callback err)))))
              (async/exec (fn [_ _] (callback nil)))))]
    (step '() keys)
    nil))

(defn run! [system keys f callback]
  {:pre [(map? system) (some-> system meta ::ig/origin)]}
  (let [keys' (#'ig/dependent-keys (#'ig/system-origin system) keys)]
    (run-loop system keys' f callback)))

(defn reverse-run! [system keys f callback]
  {:pre [(map? system) (some-> system meta ::ig/origin)]}
  (let [keys' (#'ig/reverse-dependent-keys (#'ig/system-origin system) keys)]
    (run-loop system keys' f callback)))

(defn- wrap-build-callback [resolve reject]
  (fn
    ([ret] (resolve ret))
    ([err ret] (if err (reject err) (resolve ret)))))

(defn- try-build-action [system f k v resolve reject]
  (let [reject' #(reject (#'ig/build-exception system f k v %))]
    (try
      (f k v (wrap-build-callback resolve reject'))
      (catch #?(:clj Throwable :cljs :default) t
        (reject' t)))))

(defn- build-key [f assertf system [k v] resolve reject]
  (let [v' (#'ig/expand-key system v)]
    (assertf system k v)
    (let [resolve' (fn [ret]
                     (as-> system system
                       (assoc system k ret)
                       (vary-meta system assoc-in [::ig/build k] v')
                       (resolve system)))]
      (try-build-action system f k v' resolve' reject))))

(defn build
  ([config keys f callback]
   (build config keys f (fn [_ _ _]) callback))
  ([config keys f assertf callback]
   (letfn [(step [system [kv & kvs]]
             (if kv
               (-> (async/exec (partial build-key f assertf system kv))
                   (async/then (fn [system] (step system kvs))
                               (fn [err] (callback err nil))))
               (async/exec (fn [_ _] (callback nil system)))))]
     (let [relevant-keys   (#'ig/dependent-keys config keys)]
       (step (with-meta {} {::ig/origin config})
             (map (fn [k] [k (config k)]) relevant-keys))
       nil))))

(defn init
  ([config callback]
   (init config (keys config) callback))
  ([config keys callback]
   {:pre [(map? config)]}
   (build config keys init-key callback)))

(defn halt!
  ([system callback]
   (halt! system (keys system) callback))
  ([system keys callback]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (reverse-run! system keys halt-key! callback)))

(defn- halt-missing-keys! [config system keys callback]
  (let [graph        (-> system meta ::origin ig/dependency-graph)
        missing-keys (#'ig/missing-keys system (#'ig/dependent-keys config keys))]
    (letfn [(step [[k & ks]]
              (if k
                (-> (async/exec #(halt-key! k (system k) (wrap-run-callback %1 %2)))
                    (async/then (fn [_] (step ks))
                                callback))
                (async/exec (fn [_ _] (callback nil)))))]
      (step missing-keys))))

(defn resume
  ([config system callback]
   (resume config system (keys system) callback))
  ([config system keys callback]
   {:pre [(map? config) (map? system) (some-> system meta ::ig/origin)]}
   (letfn [(cont [err]
             (if err
               (callback err)
               (build config keys
                      (fn [k v callback']
                        (if (contains? system k)
                          (resume-key k v
                                      (-> system meta ::ig/build (get k))
                                      (system k)
                                      callback')
                          (init-key k v callback')))
                      callback)))]
     (halt-missing-keys! config system keys cont))))

(defn suspend!
  ([system callback]
   (suspend! system (keys system) callback))
  ([system keys callback]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (reverse-run! system keys suspend-key! callback)))

(defn set-async-executor! [executor]
  (async/set-async-executor! executor)
  nil)
