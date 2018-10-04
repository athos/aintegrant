(ns aintegrant.core
  (:require [integrant.core :as ig]))

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

(defn- exec-async [f]
  (f))

(defn- wrap-callback [callback]
  (fn
    ([ret] (callback nil ret))
    ([err ret] (callback err ret))))

(defn- try-build-action [system f k v callback]
  (let [callback' (wrap-callback callback)]
    (try
      (f k v callback')
      (catch Throwable t
        (callback t nil)))))

(defn- build-key [f assertf system [k v] callback]
  (let [v' (#'ig/expand-key system v)]
    (assertf system k v)
    (let [callback' (fn [err ret]
                      (if err
                        (callback err nil)
                        (as-> system system
                          (assoc system k ret)
                          (vary-meta system assoc-in [::ig/build k] v')
                          (callback nil system))))]
      (try-build-action system f k v' callback'))))

(defn build
  ([config keys f callback]
   (build config keys f (fn [_ _ _]) callback))
  ([config keys f assertf callback]
   (letfn [(step [system [kv & kvs]]
             (if kv
               (exec-async
                #(build-key f assertf system kv
                            (fn [err system]
                              (if err
                                (callback err nil)
                                (step system kvs)))))
               (callback nil system)))]
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
   nil))

(defn resume
  ([config system callback]
   (resume config system keys callback))
  ([config system keys callback]
   {:pre [(map? config) (map? system) (some-> system meta ::ig/origin)]}
   (build config keys
          (fn [k v]
            (if (contains? system k)
              (resume-key k v (-> system meta ::build (get k)) (system k))
              (init-key k v)))
          callback)))

(defn suspend!
  ([system callback]
   (suspend! system (keys system) callback))
  ([system keys callback]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   nil))
