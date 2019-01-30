# Aintegrant
[![Clojars Project](https://img.shields.io/clojars/v/aintegrant.svg)](https://clojars.org/aintegrant)
[![CircleCI](https://circleci.com/gh/athos/aintegrant.svg?style=shield)](https://circleci.com/gh/athos/aintegrant)
[![codecov](https://codecov.io/gh/athos/aintegrant/branch/master/graph/badge.svg)](https://codecov.io/gh/athos/aintegrant)

Aintegrant ain't Integrant, it's Async Integrant!

In short, Aintegrant is the async version of Integrant. Particularly in ClojureScript,
application initialization may involve a various kinds of asynchronous execution,
such as Web API call, resource fetch or file I/O (on Node.js). Aintegrant will help in such cases.

Also, Aintegrant is completely compatible with Integrant: Initialization code for Integrant
perfectly runs via Aintegrant, and migration for turning some of the initialization asynchronous
usually takes just a little effort.

**Note:** Aintegrant focuses on the asynchronization of Integrant's facilities, and **NOT**
on the parallelization of them (at least for now). So, each component's `init-key`/`halt-key!` etc.
is always performed one by one.

## Installation

Add the following to the `:dependencies` in your `project.clj` or `build.boot`:

[![Clojars Project](https://clojars.org/aintegrant/latest-version.svg)](http://clojars.org/aintegrant)

## Usage

If you are not familiar with [Integrant](https://github.com/weavejester/integrant), you may want to read its document first.

Aintegrant provides the same set of APIs as Integrant's. That is, it has a custom version of `init`, `halt!`, etc. and allows users to extend them with `init-key`, `halt-key!`, etc. respectively.
Unlike Integrant, Aintegrant’s vesion of `init`, `halt!` etc. and their multimethod cousins **take a callback function as the last argument**.

For example, let’s say you have an implementation of `init-key` for the `:database` key. You can invoke the callback to return the initialized database component and notify completing initialization to trigger the next component’s initialization (if any).

```clojure
(require '[aintegrant.core :as ag]
         '[integrant.core :as ig])

(def config
  {:database {:uri "..."}})

(defmethod ag/init-key :database [_ {:keys [uri]} callback]
  (callback (connect-to-database uri)))

(defmethod ag/halt-key! :database [_ conn callback]
  (disconnect! conn)
  (callback))

(def system (atom nil))
;; for initializing
(ag/init config (fn [_ sys] (reset! system sys)))
;; for halting
(ag/halt! @system (fn [_] (reset! system nil)))
```

You can also use the callback to handle errors raised during the initialization:

```clojure
(defmethod ag/init-key :database [_ {:keys [uri]} callback]
  (try
    (let [conn (connect-to-database uri)]
      (callback conn))
    (catch Exception e
      (callback e nil))))
      
(defmethod ag/halt-key! :database [_ conn callback]
  (try
    (disconnect! conn)
    (catch Exception e
      (callback e))))
      
(ag/init config
         (fn [e sys]
           (if e
             (println (ex-message e))
             (reset! system sys))))

(ag/halt! config
          (fn [e]
            (if e
              (println (ex-message e))
              (reset! system nil))))
```

Note that the arity of callback functions differs between `ag/init-key` and `ag/halt-key!` (ie. the callback for `ag/init-key` accepts one or two arguments while the one for `ag/halt-key!` accepts zero or one argument)

When you invoke the callback for `ag/init-key` with one argument, that will be taken as the return value. And when you call it with two argument, the first will be taken as the raised error. The same goes for the callback for `ag/resume-key`.

When the callback for `ag/halt-key!` is called with zero argument, it will just notify the completion of halting without errors. When it is called with one argument, that will be taken as the raised error. The same goes for the callback for `ag/suspend-key!`.

## License

Copyright © 2018 Shogo Ohta 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
