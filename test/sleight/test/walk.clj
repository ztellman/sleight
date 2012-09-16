;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns sleight.test.walk
  (:use
    [sleight.walk]
    [clojure.test]))

(def counter (atom 0))

(defmacro with-counter [& body]
  `(do
     (try
       ~@body
       @counter
       (catch Exception e#
         @counter)
       (finally
         (reset! counter 0)))))

(defn increment-handler [handlers x]
  `(do
     (swap! counter inc)
     ~(list* (first x) (map (partial walk-exprs handlers) (rest x)))))

(defn eval* [x]
  (eval
    (walk-exprs
      (merge expr-handlers {#'+ increment-handler})
      x)))

(defn run-test-roundtrip [x]
  (is (= x (walk-exprs expr-handlers x))))

(deftest test-roundtrip
  (run-test-roundtrip `(concat [[1] #{2} {3 4}])))

(deftest test-walk-exprs

  ;; x = y * 2
  (let [f (eval*
            `(fn [x#]
               (+ x# (+ x# x#))))]
    (is (= 2 (with-counter
               (f 1))))
    (is (= 20 (with-counter
                (dotimes [_ 10]
                  (f 1))))))

  ;; x = y * 2.5
  (let [f (eval*
            `(fn [x#]
               (loop [x# x#]
                 (when (pos? x#)
                   (let [x# (+ x# (- x# x#))]
                     (when (even? x#)
                       (+ x# x#)))
                   (recur (+ x# -1))))))]
    (is (= 25 (with-counter (f 10))))
    (is (= 50 (with-counter (f 20))))))
