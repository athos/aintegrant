# Aintegrant
[![Clojars Project](https://img.shields.io/clojars/v/aintegrant.svg)](https://clojars.org/aintegrant)

Aintegrant ain't Integrant, it's Async Integrant!

In short, Aintegrant is the async version of Integrant. Particularly in ClojureScript,
application initialization may involve a various kinds of asynchronous execution,
such as Web API call, resource fetch or file I/O (on Node.js). Aintegrant will work in such cases.

Also, Aintegrant is completely compatible with Integrant: Initialization code for Integrant
perfectly runs via Aintegrant, and migration for turning some of the initialization asynchronous
usually takes just a little effort.

## Installation

Add the following to the `:dependencies` in your `project.clj` or `build.boot`:

[![Clojars Project](https://clojars.org/aintegrant/latest-version.svg)](http://clojars.org/aintegrant)

## Usage

FIXME

## License

Copyright © 2018 Shogo Ohta 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
