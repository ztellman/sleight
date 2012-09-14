A plugin for whole-program transformations via sleight.

### Usage

This plugin is only compatible with Leiningen 2.X.

To use in all projects, add `[lein-sleight "0.1.0"]` to the `:plugins` vector of your `:user` profile in `~/.lein/profiles.clj`.  To use for a specific project, add `[lein-sleight "0.1.0"]` to the `:plugins` vector in `project.clj`.

First, define a transform in your project or one of its dependencies.

```clj
(sleight.core/def-transform reverse-everything
  :pre #(println "Get ready for some confusion...")
  :transform reverse)
```

Then, in your `project.clj`, add something like this:

```clj
(project your-project "1.0.0"
  :sleight {:default {:transforms [a.namespace/reverse-everything]}
            :partial {:transforms [a.namespace/reverse-everything]
	                  :namespaces ["another.namespace*"]}})
```

The `:transforms` key maps onto a list of transforms, which are applied left to right.  The `:namespaces` key maps onto a list of namespace filters, which confines the transformation to namespaces which match one of the filters.

The `lein sleight` is not a standalone task, it's meant to modify other tasks.  For instance, if we want to apply our transform to code while testing, we'd run:

```
lein sleight test
```

Since we haven't given a selector before the `test` task, the `:default` transform is selected.  To specify the `:partial` transform, we'd run:

```
lein sleight :partial test
```

### License

Copyright © 2012 Zachary Tellman

Distributed under the Eclipse Public License, the same as Clojure.
