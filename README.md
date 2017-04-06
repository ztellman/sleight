![](docs/sleight.jpg)

Sleight allows pervasive transforms to Clojure code, akin to wrapping all your code and the code in the libraries you depend on in the same macro.

### what is this good for?

Possible uses include:

* language extensions
* pervasive inversion-of-control transforms
* automatic instrumentation
* test coverage metrics
* anything else you can dream up

### is this a good idea?

Maybe!

### how do I use it?

Sleight can be used via the `lein-sleight` plugin.  To use in all projects, add `[lein-sleight "0.2.2"]` to the `:plugins` vector of your `:user` profile in `~/.lein/profiles.clj`.

To use for a specific project, add `[lein-sleight "0.2.2"]` to the `:plugins` vector in `project.clj`.

---

Once this is done, define a transform in your project or one of its dependencies.

```clj
(def reverse-vectors
  {:pre (fn [] (println "Get ready for some confusion..."))
   :transform (fn [x] (riddley.walk/walk-exprs vector? reverse x))
   :post (fn [] (println "That probably didn't go very well"))})
```

A transform is defined as a map containing one or more of the keys `:pre`, `:transform`, and `:post`.  The `:pre` callback is invoked before the reader is hijacked to perform the transformation, the `:transform` function is passed each form as it's read it, and returns a modified form.  The `:post` callback is invoked as the process is closed.

To perform safe code transformations, use [Riddley](https://github.com/ztellman/riddley).

Then, in your `project.clj`, add something like this:

```clj
(project your-project "1.0.0"
  :sleight {:default {:transforms [a.namespace/reverse-vectors]}
            :partial {:transforms [a.namespace/reverse-vectors]
                      :namespaces ["another.namespace*"]}})
```

The `:transforms` key maps onto a list of transforms, which are applied left to right.  The `:namespaces` key maps onto a list of namespace filters, which confines the transformation to namespaces which match one of the filters.

`lein sleight` is not a standalone task, it's meant to modify other tasks.  For instance, if we want to apply our transform to code while testing, we'd run:

```
lein sleight test
```

Since we haven't given a selector before the `test` task, the `:default` transform is selected.  To specify the `:partial` transform, we'd run:

```
lein sleight :partial test
```

### license

Copyright (C) 2013 Zachary Tellman

Distributed under the MIT License
