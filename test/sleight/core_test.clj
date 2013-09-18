(ns sleight.core-test
  (:require
    [clojure.test :refer :all]))

(defn run-make-odd-test []
  (is (= 2 3))
  (is (not= 1 2))
  (is (= 2 2))
  (is (= 3 3)))

(deftest ^:make-odd test-make-odd
  (run-make-odd-test))

(defn run-normal-test []
  (is (not= 2 3))
  (is (not= 1 2))
  (is (= 2 2))
  (is (= 3 3)))

(deftest test-normal
  (run-normal-test))



