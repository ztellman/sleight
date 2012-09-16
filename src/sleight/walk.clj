;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns sleight.walk)

(declare map-entry)

(defn map-entry? [x]
  (instance? java.util.Map$Entry x))

(defn walkable? [x]
  (and
    (sequential? x)
    (not (vector? x))
    (not (map-entry? x))))

(defn- handle-expr
  [handlers name default-handler x]
  (if-let [handler (handlers name)]
    (handler handlers x)
    (default-handler x)))

(defn- term-descriptor [x]
  (or
    (and symbol? (resolve x))
    x))

(declare default-walk-handlers)

(defn walk-exprs
  ([x]
     (walk-exprs default-walk-handlers x))
  ([handlers x]
     (let [handle-expr #(handle-expr handlers %1 %2 x)
           walk-exprs (partial walk-exprs handlers)]
       (cond
      
         (walkable? x)
         (doall (handle-expr (term-descriptor (first x)) #(map walk-exprs %)))

         (map-entry? x)
         (handle-expr #'sleight.walk/map-entry #(clojure.lang.MapEntry.
                                                  (walk-exprs (key %))
                                                  (walk-exprs (val %))))
      
         (vector? x)
         (handle-expr #'clojure.core/vector #(vector (map walk-exprs %)))
      
         (map? x)
         (handle-expr #'clojure.core/hash-map #(into {} (map walk-exprs %)))
      
         (set? x)
         (handle-expr #'clojure.core/hash-set #(set (map walk-exprs %)))

         :else
         x))))

;;;

(defn- fn-handler [handlers x]
  (let [prelude (take-while (complement sequential?) x)
        remainder (drop (count prelude) x)
        multiple-arity? (->> remainder first vector? not)
        body-handler (fn [x]
                       (doall
                         (list* (first x)
                           (map (partial walk-exprs handlers) (rest x)))))]
    (concat
      prelude
      (if multiple-arity?
        (map body-handler remainder)
        (body-handler remainder)))))

(defn- let-bindings-handler [handlers x]
  (let [pairs (partition-all 2 x)]
    (->> (map second pairs)
      (map (partial walk-exprs handlers))
      (interleave (map first pairs))
      vec)))

(defn- let-handler [handlers x]
  (list*
    (first x)
    (let-bindings-handler handlers (second x))
    (map (partial walk-exprs handlers) (drop 2 x))))

(def default-walk-handlers
  {#'defn fn-handler
   #'fn fn-handler
   'fn* fn-handler
   #'let let-handler
   #'loop let-handler
   'let* let-handler
   'loop* let-handler})

