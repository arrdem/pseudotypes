# pseudotypes

A common design pattern in Clojure is to define "pseudo types", being a map with
a more or less clearly defined series of keys. Code is then written upon these
assumptions, thus transforming the map standard (from an OO point of view) to an
extremely ill-defined type. Due to lack of typechecking it is possible for the
map "objects" passed to a function to be incorrect or noncompliant with
language-unenforced standard.

Pseudotypes attempts to remedy this situation by providing a way to specify
pseudotype type-specific preconditions and postconditions to functions.

## Usage



## License

Copyright © 2013 Reid McKenzie

Distributed under the Eclipse Public License, the same as Clojure.
