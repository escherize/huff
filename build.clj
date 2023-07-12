(ns build
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.string :as str]
    [clojure.tools.build.api :as b]
    [org.corfield.build :as bb]))


(defn git-tag
  []
  (let [tag (->> (sh "git" "tag") :out str/split-lines peek)]
    (if (seq tag) tag "0.1-SNAPSHOT")))


(def lib 'io.github.escherize/huff)
(def main 'huff.core)
(def version (git-tag))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))


(defn clean
  [_]
  (b/delete {:path "target"}))


(defn jar
  [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :scm {:url "https://github.com/escherize/huff"
                      :connection "scm:git:git://github.com/escherize/huff.git"
                      :developerConnection "scm:git:ssh://git@github.com/escherize/huff.git"}
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))


(defn deploy
  "Deploy the JAR to Clojars."
  [{:as opts}]
  (-> opts
      (assoc :lib lib
             :version version
             :main main)
      (bb/deploy)))
