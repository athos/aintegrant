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

(defn init
  ([config callback]
   (init config (keys config) callback))
  ([config keys callback]
   {:pre [(map? config)]}
   nil))

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
   nil))

(defn suspend!
  ([system callback]
   (suspend! system (keys system) callback))
  ([system keys callback]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   nil))
