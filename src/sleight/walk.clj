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
    (and (symbol? x) (resolve x))
    x))

(declare expr-handlers)

(defn walk-exprs
  "Walks a code form, visiting only valid expressions.  The 'handlers' parameter is a function
   which takes the first element of the expression (which, if it can be resolved, is a var), and
   returns a function which takes the handlers parameter and the expression.

   By default, this is 'expr-handlers', which is a simple map of vars and symbols onto functions."
  ([x]
     (walk-exprs expr-handlers x))
  ([handlers x]
     (let [handle-expr #(handle-expr handlers %1 %2 x)
           walk-exprs (partial walk-exprs handlers)
           x* (cond
           
                (walkable? x)
                (doall (handle-expr (term-descriptor (first x)) #(map walk-exprs %)))
                
                (map-entry? x)
                (handle-expr #'sleight.walk/map-entry #(clojure.lang.MapEntry.
                                                         (walk-exprs (key %))
                                                         (walk-exprs (val %))))
                
                (vector? x)
                (handle-expr #'clojure.core/vector #(vec (map walk-exprs %)))
                
                (map? x)
                (handle-expr #'clojure.core/hash-map #(into {} (map walk-exprs %)))
                
                (set? x)
                (handle-expr #'clojure.core/hash-set #(set (map walk-exprs %)))
                
                :else
                x)]
       (if (instance? clojure.lang.IObj x*)
         (with-meta x* (merge (meta x) (meta x*)))
         x*))))

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

(def expr-handlers
  {#'defn fn-handler
   #'fn fn-handler
   'fn* fn-handler
   #'let let-handler
   #'loop let-handler
   'let* let-handler
   'loop* let-handler})

;;;

(defn postwalk-exprs
  [f x]
  (walk-exprs
    (fn handlers [term]
      (fn [_ x]
        (let [x* (if-let [handler (expr-handlers term)]
                   (handler handlers x)
                   (map #(postwalk-exprs f %) x))]
          (f x*))))
    x))

(defn prewalk-exprs
  [f x]
  (walk-exprs
    (fn handlers [_]
      (fn [_ x]
        (let [x* (f x)]
          (if-let [handler (-> x* first term-descriptor expr-handlers)]
            (handler handlers x*)
            (map #(prewalk-exprs f %) x*)))))
    x))

(defn macroexpand-all
  ([x]
     (macroexpand-all (constantly true) x))
  ([macro-predicate x]
     (postwalk-exprs
       #(if (-> % first term-descriptor macro-predicate)
          (macroexpand %)
          %)
       x)))


