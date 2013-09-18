(defproject sleight "0.2.0-SNAPSHOT"
  :description "whole-program transformations for clojure"
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [riddley "0.1.4"]]}}
  :plugins [[lein-sleight "0.2.0-SNAPSHOT"]]
  :sleight {:default {:transforms [sleight.transform-test/make-odd]}}
  :test-selectors {:make-odd :make-odd
                   :normal (complement :make-odd)})
