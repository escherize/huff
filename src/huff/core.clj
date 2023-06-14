(ns huff.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [malli.core :as m]))

(set! *warn-on-reflection* true)

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
               [:primative [:or :string number? :boolean :nil]]]}}
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
  (let [text-str (stringify text)]
    (if-not (and *escape?* (some char->replacement text-str))
      text-str
      (let [s (into [] (stringify text))
            sb (StringBuilder.)]
        (if *escape?*
          (do
            (doseq [c s] (.append ^StringBuilder sb
                                  (if-let [replacement (char->replacement c)]
                                    replacement
                                    c)))
            (str sb))
          s)))))

(defmulti ^:private emit (fn [_sb form] (first form)))

(defmethod emit :primative [sb [_ x]]
  (.append ^StringBuilder sb ^String (escape-html x)))

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

(defn emit-style [sb s]
  (.append ^StringBuilder sb "style=\"")
  (cond
    (map? s) (doseq [[k v] (sort-by first s)]
               [(.append ^StringBuilder sb (emit-attr-value k))
                (.append ^StringBuilder sb ":")
                (.append ^StringBuilder sb (stringify v))
                (.append ^StringBuilder sb ";")])
    (string? s) (.append ^StringBuilder sb s)
    :else (throw (ex-info "style attributes need to be a string or a map." {:s s})))
  (.append ^StringBuilder sb "\""))

(defn emit-attrs [sb attrs]
  (doseq [[k value] attrs]
    (when-not (or (contains? #{false ""} value)
                  (nil? value)
                  (and (coll? value) (empty? value)))
      (.append ^StringBuilder sb " ")
      (cond
        (= :style k)
        (emit-style sb value)

        (coll? value)
        (do (.append ^StringBuilder sb (emit-attr-value k))
            (.append ^StringBuilder sb "=\"")
            (doseq [x (interpose " " value)]
              (.append ^StringBuilder sb (escape-html x)))
            (.append ^StringBuilder sb "\""))

        :else
        (let [escaped (escape-html value)]
          (.append ^StringBuilder sb (emit-attr-value k))
          (.append ^StringBuilder sb "=\"")
          (.append ^StringBuilder sb escaped)
          (.append ^StringBuilder sb "\""))))))

;; lifted from hiccup.compiler
(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen"
  "link" "meta" "param" "source" "track" "wbr"})

(defmethod emit :tag-node-no-attrs [sb [_ {:keys [tag children]}]]
  (let [[tag tag-id tag-classes] (tag->tag+id+classes tag)
        tag-name (name tag)]
    (.append ^StringBuilder sb "<")
    (.append ^StringBuilder sb ^String tag-name)
    (when (or tag-id (not-empty tag-classes))
      (emit-attrs sb {:id tag-id :class tag-classes}))

    (if (contains? void-tags tag-name)
      (.append ^StringBuilder sb " />")
      (do
        (.append ^StringBuilder sb ">")
        (doseq [c children]
          (emit sb c))
        (.append ^StringBuilder sb "</")
        (.append ^StringBuilder sb tag-name)
        (.append ^StringBuilder sb ">")))))

(defmethod emit :tag-node [sb [_ {:keys [tag attrs children]}]]
  (let [[tag tag-id tag-classes] (tag->tag+id+classes tag)
        attrs (-> attrs
                  (update :id #(or % tag-id))
                  (update :class #(cond
                                    (string? %) (concat [%] tag-classes)
                                    (coll? %) (concat % tag-classes)
                                    (nil? %) tag-classes)))
        tag-name (name tag)]
    (.append ^StringBuilder sb "<")
    (.append ^StringBuilder sb ^String tag-name)
    (emit-attrs sb attrs)
    (if (contains? void-tags tag-name)
      (.append ^StringBuilder sb " />")
      (do (.append ^StringBuilder sb ">")
          (doseq [c children] (emit sb c))
          (.append ^StringBuilder sb "</")
          (.append ^StringBuilder sb ^String tag-name)
          (.append ^StringBuilder sb ">")))))

(defmethod emit :raw-node [sb [_ {:keys [content]}]]
  (.append ^StringBuilder sb content))

(defmethod emit :fragment-node [sb [_ {:keys [children]}]]
  (doseq [c children]
    (emit ^StringBuilder sb c)))

(defmethod emit :siblings-node [sb [_ {:keys [children]}]]
  (doseq [c children]
    (emit ^StringBuilder sb c)))

(defmethod emit :component-node [sb [_ {:keys [view-fxn children]}]]
  (emit sb (parser (apply view-fxn children))))

(defn html [h]
  (let [parsed (parser h)]
    (if (= parsed :malli.core/invalid)
      (let [{:keys [value]} (explainer h)]
        (throw (ex-info "Invalid huff form passed to html. See `huff.core/hiccup-schema` for more info" {:value value})))
      (let [sb (StringBuilder.)]
        (emit sb parsed)
        (str sb)))))

(defn page [h] (html [:<> [:hiccup/raw-html "<!doctype html>"] h]))
