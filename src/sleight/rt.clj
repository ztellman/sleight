;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns sleight.rt
  (:require
    [sleight.reader :as reader]
    [clojure.java.io :as io])
  (:import
    [java.io
     InputStreamReader
     InputStream
     FileNotFoundException]
    [clojure.lang
     RT
     Compiler]))

(defn resource [file]
  (RT/getResource (RT/baseLoader) file))

(defn ^InputStream resource->stream [file]
  (RT/resourceAsStream (RT/baseLoader) file))

(defn ^InputStreamReader resource->reader [file]
  (InputStreamReader. (resource->stream file)))

(defn file->name [^String file]
  (let [slash (.lastIndexOf file "/")]
    (if (pos? slash)
      (.substring file (inc slash))
      file)))

;; RT.compile

(defn compile* [transform ^String file]
  (let [in (resource->reader file)]
    (if in
      (try
        (Compiler/compile
          (reader/transform-reader transform in)
          file
          (file->name file))
        (finally
          (.close in)))
      (throw (FileNotFoundException. (str "Could not locate Clojure resource on classpath: " file))))))

;; RT.loadResourceScript

(defn load-resource-script* [transform ^String file]
  (let [in (resource->reader file)]
    (if in
      (try
        (Compiler/load
          (reader/transform-reader transform in)
          file
          (file->name file))
        (finally
          (.close in)))
      (throw (FileNotFoundException. (str "Could not locate Clojure resource on classpath: " name))))))

;; RT.load

(defn push-bindings []
  (let [fields [#'*ns* #'*warn-on-reflection* #'*unchecked-math*]]
    (push-thread-bindings (zipmap fields (map deref fields)))))

(defn load-class? [class-file clj-file]
  (let [class-url (resource class-file)
        clj-url (resource clj-file)]
    (and
      class-url
      (or (not clj-url)
        (> (RT/lastModified class-url class-file)
          (RT/lastModified clj-url clj-file))))))

(defn load* [transform script-base]
  (prn "rt-load*" script-base)
  (let [class-file (str script-base RT/LOADER_SUFFIX ".class")
        clj-file (str script-base ".clj")
        loaded? (when (load-class? class-file clj-file)
                  (try
                    (push-bindings)
                    (RT/loadClassForName (str (.replace script-base "/" ".") RT/LOADER_SUFFIX))
                    (finally
                      (pop-thread-bindings))))]

    (when-not loaded?
      (if (resource clj-file)
        (if @Compiler/COMPILE_FILES
          (compile* transform clj-file)
          (load-resource-script* transform clj-file))
        (throw (FileNotFoundException. (format "Could not locate %s or %s on classpath" class-file clj-file)))))))


