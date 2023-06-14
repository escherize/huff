(ns huff.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [malli.core :as m]))

;; (set! *warn-on-reflection* true)

(def ^:private hiccup-schema
  [:schema
   {:registry
    {"hiccup" [:orn
               [:fragment-node  [:catn
                                 [:fragment-indicator [:= :<>]]
                                 [:children           [:* [:schema [:ref "hiccup"]]]]]]
               [:siblings-node [:catn [:children [:* [:schema [:ref "hiccup"]]]]]]
               [:tag-node       [:catn
                                 [:tag      simple-keyword?]
                                 [:attrs    [:map-of [:or :string :keyword :symbol] :any]]
                                 [:children [:* [:schema [:ref "hiccup"]]]]]]
               [:tag-node-no-attrs [:catn
                                    [:tag      simple-keyword?]
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

(def explainer (m/explainer hiccup-schema))

(def ^:private parser (m/parser hiccup-schema))

(defn stringify [text]
  (cond
    (nil? text) ""
    (simple-keyword? text) (name text)
    (keyword? text) [(namespace text) "/" (name text)]
    (ratio? text) (str (double text))
    :else (str text)))

(def ^:dynamic *escape?* true)

(def char->replacement
  {\& "&amp;"
   \< "&lt;"
   \> "&gt;"
   \" "&quot;"
   \\ "&#39;"})

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (let [s (into [] (stringify text))
        sb (StringBuilder.)]
    (if *escape?*
      (do
        (doseq [c s] (.append ^StringBuilder sb
                              (if-let [replacement (char->replacement c)]
                                replacement
                                c)))
        (str sb))
      s)))

(defmulti ^:private emit first)

(defmethod emit :primitive [[_ x]] (escape-html x))

(defn step
  "Used to extract :.class.names.and#ids from keywords."
  [{:keys [mode seen] :as acc} char]
  (case mode
    :tag (cond (= char \#) (assoc acc :tag (str/join seen) :seen [] :mode :id)
               (= char \.) (assoc acc :tag (if (empty? seen) "div" (str/join seen)) :seen [] :mode :class)
               :else (update acc :seen conj char))
    :id (cond (= char \#) (throw (ex-info "can't have 2 #'s in a tag." {:acc acc}))
              (= char \.) (assoc acc :id (str/join seen) :seen [] :mode :class) :else (update acc :seen conj char))
    :class (cond (= char \#) (-> acc
                                 (update :class conj (str/join seen))
                                 (assoc :seen [] :mode :id))
                 (= char \.) (-> acc
                                 (update :class conj (str/join seen))
                                 (assoc :seen [] :mode :class))
                 :else (update acc :seen conj char))))

(defn tag->tag+id+classes [tag]
  (-> (reduce step
              {:mode :tag :class [] :seen [] :id nil}
              (name tag))
      (step \.) ;; move "seen " into the right place
      (map [:tag :id :class])))

(defn- emit-attr-value [k]
  (str/replace (name k) #"-([a-z])" (fn [[_ char]] (str/upper-case char))))

(defn emit-style [s]
  ["style=\""
   (cond
     (map? s) (for [[k v] (sort-by first s)]
                [(emit-attr-value k) ":" (stringify v) ";"])
     (string? s) s
     :else (throw (ex-info "style attributes need to be a string or a map." {:s s})))
   "\""])

(defn emit-attrs [p]
  (let [outs (keep (fn [[k value]]
                     (when-not (or (contains? #{false ""} value) (nil? value)
                                   (and (coll? value) (empty? value)))
                       (cond
                         (= :style k)
                         (emit-style value)

                         (coll? value)
                         [(emit-attr-value k) "=\""  (escape-html (str/join " " value)) "\""]

                         :else
                         (let [escaped (escape-html value)]
                           [(emit-attr-value k) "=\"" escaped "\""]))))
                   p)]
    [(when (seq outs) " ")
     (interpose " " (sort outs))]))

;; lifted from hiccup.compiler
(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen"
  "link" "meta" "param" "source" "track" "wbr"})

(defmethod emit :tag-node-no-attrs [[_ {:keys [tag children]}]]
  (let [[tag tag-id tag-classes] (tag->tag+id+classes tag)
        tag-name (name tag)]
    (if (contains? void-tags tag-name)
      (list "<" tag-name (when (or tag-id (not-empty tag-classes))
                           (emit-attrs {:id tag-id :class tag-classes})) " />")
      (list "<" tag-name (when (or tag-id (not-empty tag-classes))
                           (emit-attrs {:id tag-id :class tag-classes})) ">"
            (map emit children)
            (list "</" tag-name ">")))))

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
      (list "<" tag-name (emit-attrs attrs) " />")
      (list "<" tag-name (emit-attrs attrs) ">"
            (map emit children)
            (list "</" tag-name ">")))))

(defmethod emit :raw-node [[_ {:keys [content]}]] content)

(defmethod emit :fragment-node [[_ {:keys [children]}]]
  (map emit children))

(defmethod emit :siblings-node [[_ {:keys [children]}]]
  (map emit children))

(declare html)

(defmethod emit :component-node [[_ {:keys [view-fxn children]}]]
  (emit (parser (apply view-fxn children))))

(declare re-string)

(defn re-string
  ([iolist] (let [sb (StringBuilder. (* 10 (count iolist)))]
         (str (re-string sb iolist))))
  ([sb h]
   (reduce
     (fn [sb iolist]
       (cond
         (string? iolist) (.append ^StringBuilder sb ^String iolist)
         (coll? iolist) (re-string sb iolist)
         (nil? iolist) sb
         (char? iolist) (.append ^StringBuilder sb ^Character iolist)
         :else (throw (ex-info "weird" {:h iolist}))))
     sb h)))

(defn html [h]
  (let [parsed (parser h)]
    (if (= parsed :malli.core/invalid)
      (let [{:keys [value]} (explainer h)]
        (throw (ex-info "Invalid huff form passed to html. See `huff.core/hiccup-schema` for more info" {:value value})))
      (re-string (emit parsed)))))

(defn page [h] (html [:<> [:hiccup/raw-html "<!doctype html>"] h]))
