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

(def pom-template
  [[:description "A library for cozy and delightful html generation."]
   [:url "https://github.com/escherize/huff"]
   [:licenses
    [:license
     [:name "Eclipse Public License"]
     [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
   [:developers
    [:developer
     [:name "Bryan Maass"]]]])

(defn jar
  [_]
  (b/write-pom {:basis basis
                :class-dir class-dir
                :target "target"
                :lib lib
                :version version
                :scm {:url "https://github.com/escherize/huff"
                      :connection "scm:git:git://github.com/escherize/huff.git"
                      :developerConnection "scm:git:ssh://git@github.com/escherize/huff.git"}
                :src-dirs ["src"]
                :resource-dirs ["resources"]
                :pom-data pom-template})
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
