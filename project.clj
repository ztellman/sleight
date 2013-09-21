(defproject sleight "0.2.1-SNAPSHOT"
  :description "whole-program transformations for clojure"
  :dependencies [[org.clojars.trptcolin/sjacket "0.1.0.3"
                  :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [riddley "0.1.5-SNAPSHOT"]]}}
  :plugins [[lein-sleight "0.2.0"]]
  :sleight {:default {:transforms [sleight.transform-test/make-odd]}
            :identity {:transforms []}}
  :test-selectors {:make-odd :make-odd
                   :default (complement :make-odd)})
