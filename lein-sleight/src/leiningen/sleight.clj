;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns leiningen.sleight
  (:require [leinjacker.eval :as eval]
            [leiningen.core.project :as project]))

(defn arguments [args]
  (if (and (first args)
         (.startsWith ^String (first args) ":"))
    [(.substring (first args) 1) (second args) (drop 2 args)]
    ["default" (first args) (rest args)]))

(defn update-project-dependencies
  [project]
  (let [profile-name (-> (gensym) name keyword)
        added-profile (project/add-profiles project
                                            {profile-name
                                             {:dependencies [['sleight "0.2.0-SNAPSHOT"]]}})
        merged-profile (project/merge-profiles added-profile [profile-name])]
    merged-profile))

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

(defn new-eval-in-project [{:keys [transforms namespaces]}]
  (fn [eip project form pre-form]
    (eip
      project
      `(do
         ~(switch-form transforms namespaces)
         ~form)
      `(do
         ~(load-form transforms)
         ~pre-form))))

(defn add-built-ins [sleight-options]
  (merge
    {:identity {:transforms ['sleight.core/identity-transform]}}
    sleight-options))

(defn sleight
  [project & args]
  (let [[transform-name task args] (arguments args)
        transform (-> project
                    :sleight
                    add-built-ins
                    (get (keyword transform-name)))]

    ;; make sure the reader switch occurs in the sub-task
    (if transform
      (-> transform
        new-eval-in-project
        eval/hook-eval-in-project)
      (println (str "No sleight transform defined for " (keyword transform-name) ", skipping.")))

    ;; run the sub-task
    (eval/apply-task task (update-project-dependencies project) args)))

