(ns aintegrant.async.core)

(defprotocol AsyncExecutor
  (-exec [this f]))

(defprotocol AsyncTask
  (-then [this resolve reject]))
