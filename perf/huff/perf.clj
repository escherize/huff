(ns huff.perf
  (:require
   [criterium.core :as bench]
   [huff.core :as h]
   [taoensso.tufte :as tufte :refer (defnp p profiled profile)]))

;; `page.edn` is wikipedia's list of common misconceptions passed through html2hiccup.

;; we support fragments:
(defonce page-edn (read-string (str "[:<> " (slurp "resources/page.edn") "]")))

(defn avg [xs] (/ (reduce + xs) (count xs)))

(comment
  (tufte/add-basic-println-handler! {})
  (profile {} (h/html page-edn)))

(comment

  (require '[hiccup.core :as hiccup])

  ;; drop 1 because no fragment support, but lists do work:
  (time (def oo (hiccup/html (drop 1 page-edn))))

  (do
    (println "### Benching hiccup\n```")
    (bench/bench (hiccup/html (drop 1 page-edn)))
    (println "```\n### Benching huff\n```")
    (bench/bench (h/html page-edn))
    (println "```"))

  (try (h/html {:a :b})
       (catch Exception e [(ex-message e) (ex-data e)]))
;; => ["Invalid huff form passed to html. See `huff.core/hiccup-schema` for more info"
;;     {:value {:a :b}}]
  )
