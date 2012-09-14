;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns leiningen.sleight
  (:use
    [leiningen.core main eval]))

(defn arguments [args]
  (if (and (first args)
         (.startsWith ^String (first args) ":"))
    [(.substring (first args) 1) (second args) (drop 2 args)]
    ["default" (first args) (rest args)]))

(defn update-project-dependencies [project]
  (update-in project [:dependencies]
    (fn [dependencies]
      (if (->> dependencies (map first) (some #{'sleight}))
        dependencies
        (conj dependencies ['sleight "0.1.0"])))))

(defn switch-form [transforms namespaces]
  `(sleight.core/switch-reader
     (sleight.core/merge-transforms
       ~transforms
       ~namespaces)))

(defn load-form [transforms]
  `(do
     (require 'sleight.core)
     ~@(->> transforms
         (map namespace)
         (remove nil?)
         (map symbol)
         (map (fn [ns] `(require '~ns))))))

(defn switch-eval-in-project [{:keys [transforms namespaces]}]
  (alter-var-root #'leiningen.core.eval/eval-in-project
    (fn [eval-in-project]
      (fn [& [project form pre-form]]
        (eval-in-project
          (update-project-dependencies project)
          `(do
             ~(switch-form transforms namespaces)
             ~form)
          `(do
             ~(load-form transforms)
             ~pre-form))))))

(defn add-built-ins [sleight-options]
  (merge
    {:identity {:transforms ['sleight.core/identity-transform]}}
    sleight-options))

(defn sleight
  [project & args]
  (let [[transform-name task args] (arguments args)]

    ;; make sure the reader switch occurs in the sub-task
    (switch-eval-in-project
      (-> project
        :sleight
        add-built-ins
        (get (keyword transform-name))))

    ;; run the sub-task
    (apply (resolve-task task) project args)))

