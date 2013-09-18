(ns sleight.core-test
  (:require
    [clojure.test :refer :all]))

(deftest ^:make-odd test-make-odd
  (is (= 2 3))
  (is (not= 1 2))
  (is (= 2 2))
  (is (= 3 3)))

(deftest test-normal
  (is (not= 2 3))
  (is (not= 1 2))
  (is (= 2 2))
  (is (= 3 3)))



