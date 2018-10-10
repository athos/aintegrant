(ns aintegrant.async
  (:import [java.util.concurrent Future]))

(defprotocol AsyncHandler
  (-exec [this f]))

(defprotocol AsyncTask
  (-then [this callback]))

(extend-protocol AsyncTask
  Future
  (-then [this callback]
    (try
      (let [v @this]
        (callback nil v))
      (catch Throwable t
        (callback t nil)))))

(defn default-async-handler []
  (reify AsyncHandler
    (-exec [this f]
      (future (f (fn
                   ([err]
                    (when err
                      (throw err)))
                   ([err ret]
                    (when err
                      (throw err))
                    ret)))))))

(def ^:private async-handler-impl
  (atom (default-async-handler)))

(defn async-handler []
  @async-handler-impl)

(defn set-async-handler! [handler]
  {:pre (satisfies? AsyncHandler handler)}
  (reset! async-handler-impl handler))

(defn exec [f]
  (-exec (async-handler) f))

(defn then [task callback]
  (-then task callback))
