(ns sleight.reader
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io
     Writer
     Reader
     StringReader
     PushbackReader]
    [clojure.lang
     LispReader
     LineNumberingPushbackReader]))

;;;

(def ^:dynamic *newlines* nil)

(def switched-printer
  (delay
    (alter-var-root #'clojure.core/pr-on
      (fn [pr-on]
        (fn [x ^Writer w]
          
          ;; special print instructions
          (when *newlines*
            (.write w (*newlines* (-> x meta :line)))
            (when-let [m (meta x)]
              (.write w (str "^" (pr-str m) " "))))
          
          (pr-on x w))))))

;;;

(defn ->line-numbering-reader [r]
  (if (instance? LineNumberingPushbackReader r)
    r
    (LineNumberingPushbackReader. r)))

(defn reader->forms [r]
  (let [r (->line-numbering-reader r)]
    (->> #(LispReader/read r false ::eof false)
      repeatedly
      (take-while #(not= ::eof %)))))

;;;

(defn newline-generator []
  (let [counter (atom 1)]
    (fn [current-line]
      (if-not current-line
        ""
        (let [diff (max 0 (- current-line @counter))]
          (swap! counter + diff)
          (->> "\n" (repeat diff) (apply str)))))))

(defn line-and-meta-preserving-pr-str [newlines x]
  (binding [*newlines* newlines
            *print-dup* true]
    (str
      (when-let [m (meta x)]
        (str "^" (pr-str m) " "))
      (pr-str x))))

;;;

(defn lazy-reader-seq [s]
  (let [s (atom s)]
    (proxy [Reader] []
      (close []
        )
      (read [cbuf offset len]
        (if-let [^Reader r (first @s)]
          (let [c (.read r)]
            (if (= -1 c)
              (do
                (swap! s rest)
                (.read this cbuf offset len))
              (do
                (aset cbuf offset (char c))
                1)))
          -1)))))

(defn dechunk [s]
  (when-not (empty? s)
    (cons
      (first s)
      (lazy-seq
        (dechunk (rest s))))))

;;;

(defn transform-reader [transform r]
  (let [_ @switched-printer ;; prime the switched printer
        newlines (newline-generator)]
    (->> r
      reader->forms
      dechunk
      (map transform)
      (map #(line-and-meta-preserving-pr-str newlines %))
      (map #(StringReader. %))
      lazy-reader-seq
      LineNumberingPushbackReader.)))
