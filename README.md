# Aintegrant
[![Clojars Project](https://img.shields.io/clojars/v/aintegrant.svg)](https://clojars.org/aintegrant)
[![CircleCI](https://circleci.com/gh/athos/aintegrant.svg?style=shield)](https://circleci.com/gh/athos/aintegrant)
[![codecov](https://codecov.io/gh/athos/aintegrant/branch/master/graph/badge.svg)](https://codecov.io/gh/athos/aintegrant)

Aintegrant ain't Integrant, it's Async Integrant!

In short, Aintegrant is the async version of Integrant. Particularly in ClojureScript,
application initialization may involve a various kinds of asynchronous execution,
such as Web API call, resource fetch or file I/O (on Node.js). Aintegrant will work in such cases.

Also, Aintegrant is completely compatible with Integrant: Initialization code for Integrant
perfectly runs via Aintegrant, and migration for turning some of the initialization asynchronous
usually takes just a little effort.

**Note:** Aintegrant focuses on the asynchronization of Integrant's app initialization facility, and NOT
on the parallelization of that (at least for now). So, each component's `init-key`/`halt-key!` etc.
is always performed one by one.

## Installation

Add the following to the `:dependencies` in your `project.clj` or `build.boot`:

[![Clojars Project](https://clojars.org/aintegrant/latest-version.svg)](http://clojars.org/aintegrant)

## Usage

FIXME

## License

Copyright © 2018 Shogo Ohta 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
