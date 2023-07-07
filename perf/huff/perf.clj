(ns huff.perf
  (:require
   [clojure.java.io :as io]
   [criterium.core :as bench]
   [hiccup.core :as hiccup]
   [huff.core :as h]
   [taoensso.tufte :as tufte :refer (defnp p profiled profile)]))

;; `page.edn` is wikipedia's list of common misconceptions passed through html2hiccup.

;; we support fragments:
(defonce big-page (read-string (str "[:<> " (slurp (io/resource "big_page.edn")) "]")))
(defonce medium-page (read-string (str "[:<> " (slurp (io/resource "medium_page.edn")) "]")))


(defn avg [xs] (/ (reduce + xs) (count xs)))

(defn run-big-hiccup [] (do (hiccup/html (drop 1 big-page)) :done))
(defn run-medium-hiccup [] (do (hiccup/html (drop 1 medium-page)) :done))

(println "# Benching with big-page")
(println "### hiccup")
(println "```")
(bench/bench (hiccup/html (drop 1 big-page)))
(println "```")
(println "### pre-compiled hiccup template")
(println "```")
(bench/bench (run-big-hiccup))
(println "```")
(println "### huff")
(println "```")
(bench/bench (h/html big-page))
(println "```")

(println "# Benching with medium_page")
(println "### hiccup")
(println "```")
(bench/bench (hiccup/html (drop 1 medium-page)))
(println "```")
(println "### pre-compiled hiccup template")
(println "```")
(bench/bench (run-medium-hiccup))
(println "```")
(println "### huff")
(println "```")
(bench/bench (h/html medium-page))
(println "```")


(comment
  (tufte/add-basic-println-handler! {})
  (profile {} (h/html big-page)))
