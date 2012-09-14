(ns leiningen.sleight
  (:use
    [leiningen.core main eval]))

(defn arguments [args]
  (if (and (first args)
         (.startsWith ^String (first args) ":"))
    [(first args) (second args) (drop 2 args)]
    [:default (first args) (rest args)]))

(defn sleight
  [project & args]
  (let [[transform task args] (arguments args)]
    (eval-in-project (update-in project [:dependencies] conj ['sleight "0.1.0-SNAPSHOT"])
      `(sleight.core/hijack-reader :transform identity)
      `(require 'sleight.core))
    (apply (resolve-task task) project args)))

