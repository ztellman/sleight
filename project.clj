(defproject sleight "0.2.2"
  :description "whole-program transformations for clojure"
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0-RC4"]
                                  [riddley "0.1.12"]]}}
  :plugins [[lein-sleight "0.2.2"]]
  :sleight {:default {:transforms [sleight.transform-test/make-odd]}
            :identity {:transforms []}}
  :test-selectors {:make-odd :make-odd
                   :default (complement :make-odd)})
