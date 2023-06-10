(ns huff.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [malli.core :as m]))

(def ^:private hiccup-schema
  [:schema
   {:registry
    {"hiccup" [:orn
               [:fragment-node  [:catn
                                 [:fragment-indicator [:= :<>]]
                                 [:children           [:* [:schema [:ref "hiccup"]]]]]]
               [:tag-node       [:catn
                                 [:tag      simple-keyword?]
                                 [:attrs    [:? [:map-of [:or :string :keyword :symbol] :any]]]
                                 [:children [:* [:schema [:ref "hiccup"]]]]]]
               ;; Always passed through untouched
               [:raw-node       [:catn
                                 [:raw     [:= :hiccup/raw-html]]
                                 [:content :string]]]
               [:component-node [:catn
                                 [:view-fxn [:and
                                             [:function [:=> [:cat :any]
                                                         [:schema [:ref "hiccup"]]]]
                                             [:not keyword?]
                                             [:not vector?]]]
                                 [:children [:* :any]]]]
               [:primitive [:or :string number? :boolean :nil]]]}}
   "hiccup"])

(let [validator (m/validator hiccup-schema)]
  (defn valid? [h] (validator h)))

(def ^:private parse (m/parser hiccup-schema))

(defn stringify [text]
  (case (pr-str (type text))
    "nil" ""
    "clojure.lang.Keyword" (if-let [ns (namespace text)]
                           (str ns "/" (name text))
                           (name text))
    "clojure.lang.Ratio" (str (double text))
    (str text)))

(def ^:dynamic *escape?* true)

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (let [s (stringify text)]
    (if *escape?*
      (-> s
          (str/replace "&"  "&amp;")
          (str/replace "<"  "&lt;")
          (str/replace ">"  "&gt;")
          (str/replace "\"" "&quot;")
          (str/replace "'" "&#39;"))
      s)))

(defmulti ^:private emit
  #_(fn [& xs] (prn ["emit" xs]) (first xs))
  first)

(defmethod emit :primitive [[_ x]] (escape-html x))

(defn step
  "Used to extract :.class.names.and#ids from keywords."
  [{:keys [mode seen] :as acc} char]
  (case mode
    :tag (cond (= char \#) (assoc acc
                                  :tag (str/join seen)
                                  :seen []
                                  :mode :id)
               (= char \.) (assoc acc
                                  :tag (if (empty? seen) "div" (str/join seen))
                                  :seen []
                                  :mode :class)
               :else (update acc :seen conj char))
    :id (cond (= char \#) (throw (ex-info "can't have 2 #'s in a tag." {:acc acc}))
              (= char \.) (assoc acc
                                 :id (str/join seen)
                                 :seen []
                                 :mode :class)
              :else (update acc :seen conj char))
    :class (cond (= char \#) (-> acc
                                 (update :classes conj (str/join seen))
                                 (assoc
                                   :seen []
                                   :mode :id))
                 (= char \.) (-> acc
                                 (update :classes conj (str/join seen))
                                 (assoc
                                   :seen []
                                   :mode :class))
                 :else (update acc :seen conj char))))

(defn tag->tag+id+classes [tag]
  (-> (reduce step
              {:mode :tag :classes [] :seen [] :id nil}
              (name tag))
      (step \.) ;; move "seen " into the right place
      (mapv [:tag :id :classes])))

(defn emit-style [s]
  (str "style=\""
       (cond
         (map? s) (str/join (for [[k v] (sort-by first s)] (str (stringify k) ":" (stringify v) ";")))
         (string? s) s
         :else (throw (ex-info "style attributes need to be a string or a map." {:s s})))
       "\""))

(emit-attrs p)

(defn emit-attrs [p]
  (def p p)
  (let [outs (keep (fn [[k value]]
                     (when-not (or (contains? #{false ""} value) (nil? value)
                                   (and (coll? value) (empty? value)))
                       (cond
                         (= :style k)
                         (emit-style value)

                         (coll? value)
                         (str (name k) "=\""  (escape-html (str/join " " value)) "\"")

                         :else
                         (let [escaped (escape-html value)]
                           (str (name k) "=\"" escaped "\"")))))
                   p)]
    (str (when (seq outs) " ")
         (str/join " " (sort outs)))))

;; lifted from hiccup.compiler
(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen"
  "link" "meta" "param" "source" "track" "wbr"})

(defmethod emit :tag-node [[_ {:keys [tag attrs children]}]]
  (let [[tag tag-id tag-classes] (tag->tag+id+classes tag)
        attrs (-> attrs
                  (update :id #(or % tag-id))
                  (update :class #(cond
                                    (string? %) (concat [%] tag-classes)
                                    (coll? %) (concat % tag-classes)
                                    (nil? %) tag-classes)))
        tag-name (name tag)]
    (if (contains? void-tags tag-name)
      (str "<" tag-name (emit-attrs attrs) " />")
      (str "<" tag-name (emit-attrs attrs) ">"
           (str/join (map emit children))
           (str "</" tag-name ">")))))

(defmethod emit :raw-node [[_ {:keys [content]}]] content)

(defmethod emit :fragment-node [[_ {:keys [children]}]]
  (str/join (mapv #(emit %) children)))

(defmethod emit :hiccup-coll [[_ {:keys [children]}]]
  (str/join (mapv emit children)))

(defn- list->fragment [x]
  (walk/postwalk
    (fn [x]
      (cond->> x
        (#{clojure.lang.PersistentList clojure.lang.LazySeq} (type x)) (into [:<>])))
    x))

(def explainer (m/explainer hiccup-schema))

(defn html [h]
  (let [parsed (-> h list->fragment parse)]
    (if (= parsed :malli.core/invalid)
      (let [{:keys [value]} (explainer h)]
        (throw (ex-info "Invalid huff form passed to html. See `huff.core/hiccup-schema` for more info" {:value value})))
      (emit parsed))))

(defmethod emit :component-node [[_ {:keys [view-fxn children]}]]
  (html (apply view-fxn children)))

(defn page [h] (html [:<> [:hiccup/raw-html "<!doctype html>"] h]))
