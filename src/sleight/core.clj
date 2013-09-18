(ns sleight.core
  (:require
    [sleight.rt :as rt]))

;;; adapted from clojure.core

(def ^:private check-cyclic-dependency #'clojure.core/check-cyclic-dependency)
(def ^:private root-resource #'clojure.core/root-resource)
(def ^:private root-directory #'clojure.core/root-directory)

(defn- load*
  [transform & paths]
  (doseq [^String path paths]
    (let [^String path (if (.startsWith path "/")
                          path
                          (str (root-directory (ns-name *ns*)) \/ path))]
      (check-cyclic-dependency path)
      (let [pending-paths @#'clojure.core/*pending-paths*]
        (when-not (= path (first pending-paths))
          (with-bindings {#'clojure.core/*pending-paths* (conj pending-paths path)}
            (rt/load* transform (.substring path 1))))))))

(def ^:private original-load @#'clojure.core/load)
(def ^:private original-eval @#'clojure.core/eval)

;;;

(defn- merge-functions [merge-fn a b]
  (cond
    (and a (not b)) a
    (and b (not a)) b
    (and a b) (merge-fn a b)
    :else nil))

(defn- filtered-transform [namespaces transform]
  (when transform
    (if namespaces
      (let [namespace-regex (->> namespaces
                              (map #(str "^" (.replace % "*" ".*")))
                              (interpose "|")
                              (apply str)
                              re-pattern)]
        (fn [x]
          (if (re-find namespace-regex (name (ns-name *ns*)))
            (transform x)
            x)))
      transform)))

(defn merge-transforms
  "Given a list of transform descriptors and namespaces in which they should be applied,
   returns a merged descriptor which will only transform code within the specified namespaces.

   If `namespaces` is nil, transforms will be applied to all namespaces."
  [transforms namespaces]
  (->> transforms
    (map #(update-in % [:transform] (partial filtered-transform namespaces)))
    (reduce
      (fn [a b]
        {:pre (merge-functions #(do (%1) (%2)) (:pre a) (:pre b))
         :post (merge-functions #(do (%1) (%2)) (:post a) (:post b))
         :transform (merge-functions comp (:transform b) (:transform a))})
      {})))

(defn wrap-reader
  "Takes a descriptor of a code transform consisting of a `:pre` no-arg callback that is
   invoked before the transform, a `:transform` function which takes a form and returns
   a modified form, and a `:post` no-arg callback which is invoked when the process is
   terminated.  All values are optional."
  [{:keys [pre post transform]}]

  (when pre
    (pre))

  (when transform

    (alter-var-root #'clojure.core/load
      (constantly (partial load* transform)))

    (alter-var-root #'clojure.core/eval
      (fn [eval]
        (fn [form]
          (eval (transform form))))))

  (when post
    (.addShutdownHook (Runtime/getRuntime)
      (Thread. post))))

(defn unwrap-reader
  "Returns the reader to its original state, undoing all invocations to wrap-reader."
  []
  (alter-var-root #'clojure.core/load (constantly original-load))
  (alter-var-root #'clojure.core/eval (constantly original-eval)))

(def identity-transform
  {:transform identity})
