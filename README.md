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

    => (use 'me.arrdem.pseudotypes)
    nil
    => (defpseudotype mymap :needs #{:foo :bar} :allows #{:baz} :foo integer? :bar vector?)
    user/mymap

For some pseudo-type name, in this case "mymap", defpseudotype creates two
functions, the predicate <name>? and the conversion function <name>.

    => (mymap? {:foo 1 :bar [2]})
    true
    => (mymap? {:foo 1 :bar [2] :baz 3.14159})
    true
    => (mymap? {:foo 1 :bar [2] :baz 3.14159 :not-defined "MWUHAHAHA"})
    true
    => (mymap {:foo 1 :bar [2] :baz 3.14159 :not-defined "MWUHAHAHA"})
    {:foo 1 :bar [2] :baz 3.14159}

Note that the conversion function discards the extra data in the argument
pseudo-object, and returns a map containing only the map keys :need ed or
:allow ed by the `(defpseudotype)`.

Now this is all well and good, except I want to use this for typechecking.
Okay fine, so I provide the macro `(deftypedfn)`.

    => (deftypedfn add1 integer [x number] (+ 1 x))
    user/add1
    => (add1 1)
    2
    => (add1 2)
    3
    => (add1 3.1)
    AssertionError Assert failed: (integer? %)  user/add1 (NO_SOURCE_FILE:1)

Well look at that... the return type is checked! That's why I specified that
add1 was "integer" when I defined the function. How about the argument x...

    => (add1 [])
    AssertionError Assert failed: (number? x)  user/add1 (NO_SOURCE_FILE:1)

Yay! Argument and return values are typechecked! And just for grins how about
doing all this with a pseudo-type...

    => (deftypedfn setfoo mymap [m mymap x number] (assoc m :foo x))
    user/setfoo
    => (setfoo {:foo 1 :bar [2]} 2)
    {:foo 2 :bar [2]}
    => (setfoo {:foo 1 :bar [2]} 2.0)
    AssertionError Assert failed: (mymap? %) user/setfoo (NO_SOURCE_FILE:1)

Remember, the `:foo` key was defined to be an integer and 2.0 is not an integer
value so that map is not `(mymap?)` true!

And just to round out this little demo I'll show one last case:

    => (setfoo {:foo 1} 2.0)
    AssertionError Assert failed: (mymap? m) user/setfoo (NO_SOURCE_FILE:1)

Yep, the map `{:foo 1}` does not conform to the mymap standard.

## License

Copyright © 2013 Reid McKenzie

Distributed under the Eclipse Public License, the same as Clojure.
