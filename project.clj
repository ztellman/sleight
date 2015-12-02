(defproject sleight "0.2.1"
  :description "whole-program transformations for clojure"
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [riddley "0.1.11"]]}}
  :plugins [[lein-sleight "0.2.1"]]
  :sleight {:default {:transforms [sleight.transform-test/make-odd]}
            :identity {:transforms []}}
  :test-selectors {:make-odd :make-odd
                   :default (complement :make-odd)})
