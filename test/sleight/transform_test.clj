(ns sleight.transform-test
  (:require
    [riddley.walk :as r]))

(def make-odd
  {:transform (fn [x]
                (r/walk-exprs
                  #(and (number? %) (even? %))
                  inc
                  x))})
