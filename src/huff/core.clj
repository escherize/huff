(ns huff.core
  (:require
   [clojure.string :as str]
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

(defn stringify
  "Take a primative, and turn it into a string."
  [text]
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

(defn maybe-escape-html
  "1. Change special characters into HTML character entities when *escape?* otherwise don't change text.
   2. Append the maybe-transformed text value onto a stringbuilder"
  [^StringBuilder sb text]
  (let [text-str (stringify text)]
    (if-not *escape?*
      (.append sb text-str)
      (let [some-replacement? (some char->replacement text-str)]
        (if some-replacement?
          (let [s (into [] text-str)]
            (doseq [c s] (.append sb (char->replacement c c))))
          (.append sb text-str))))))

(defmulti ^:private emit (fn [_sb form opts] (first form)))

(defmethod emit :primative [sb [_ x] _opts]
  (maybe-escape-html sb x))

(defn- empty-or-div [seen] (if (empty? seen) "div" (str/join seen)))

(defn- emit-style [^StringBuilder sb s]
  (.append sb "style=\"")
  (cond
    (map? s) (doseq [[k v] (sort-by first s)]
               [(.append sb (stringify k))
                (.append sb ":")
                (.append sb (stringify v))
                (when (number? v) (.append sb "px"))
                (.append sb ";")])
    (string? s) (.append sb s)
    :else (throw (ex-info "style attributes need to be a string or a map." {:s s})))
  (.append sb "\""))

(defn step
  "Used to extract :.class.names.and#ids from keywords."
  [{:keys [mode seen] :as acc} char]
  (case mode
    :tag (cond (= char \#) (assoc acc :tag (empty-or-div seen) :seen [] :mode :id)
               (= char \.) (assoc acc :tag (empty-or-div seen) :seen [] :mode :class)
               :else (update acc :seen conj char))
    :id (cond (= char \#) (throw (ex-info "can't have 2 #'s in a tag." {:acc acc}))
              (= char \.) (assoc acc :id (str/join seen) :seen [] :mode :class) :else (update acc :seen conj char))
    :class (cond (= char \#) (-> acc
                                 (update :class (fn [c] (cond-> c (not-empty seen) (conj (str/join seen)))))
                                 (assoc :seen [] :mode :id))
                 (= char \.) (-> acc
                                 (update :class (fn [c] (cond-> c (not-empty seen) (conj  (str/join seen)))))
                                 (assoc :seen [] :mode :class))
                 :else (update acc :seen conj char))))

(defn- tag->tag+id+classes* [tag]
  (-> (reduce step
              {:mode :tag :class [] :seen [] :id nil}
              (name tag))
      (step \.) ;; move "seen " into the right place
      (map [:tag :id :class])))

(defn- tag->tag+id+classes [tag]
  (mapv (comp tag->tag+id+classes* keyword) (str/split (name tag) #">")))

(defn- emit-attrs [^StringBuilder sb attrs]
  (doseq [[k value] attrs]
    (when-not
        (or (contains? #{"" nil false} value)
            (and (coll? value) (empty? value)))
      (.append sb " ")
      (cond
        (= :style k)
        (emit-style sb value)

        (coll? value)
        (do (.append sb (stringify k))
            (.append sb "=\"")
            (doseq [x (interpose " " value)] (maybe-escape-html sb x))
            (.append sb "\""))

        :else
        (do (.append sb (stringify k))
            (.append sb "=\"")
            (maybe-escape-html sb value)
            (.append sb "\""))))))

;; lifted from hiccup.compiler
(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen"
  "link" "meta" "param" "source" "track" "wbr"})

(defmethod emit :tag-node-no-attrs [^StringBuilder sb [_ {:keys [tag children]}] opts]
  (let [tag-infos (tag->tag+id+classes tag)]
    ;; emit opening tags:
    (doseq [[tag tag-id tag-classes] tag-infos]
      (let [tag-classes' (remove str/blank? tag-classes)]
        (.append sb "<")
        (.append sb (name tag))
        (when (or tag-id (not-empty tag-classes'))
          (emit-attrs sb {:id tag-id :class tag-classes'}))
        (if (contains? void-tags (name tag))
          (.append sb " />")
          (.append sb ">"))))
    ;;children
    (doseq [c children] (emit sb c opts))
    ;;closing tags
    (doseq [[tag] (reverse tag-infos)]
      (when-not (contains? void-tags (name tag))
        (.append sb "</")
        (.append sb (name tag))
        (.append sb ">")))))

(defmethod emit :tag-node [^StringBuilder sb [_ {:keys [tag attrs children]}] opts]
  (let [tag-infos (tag->tag+id+classes tag)
        [_final-tag final-tag-id final-tag-classes] (last tag-infos)
        attrs (-> attrs
                  (update :id #(or % final-tag-id))
                  (update :class #(->>
                                    (cond
                                      (string? %) (concat [%] final-tag-classes)
                                      (coll? %) (concat % final-tag-classes)
                                      (nil? %) final-tag-classes)
                                    (remove str/blank?))))
        ;; attrs go on the last tag-info:
        tag-infos' (update tag-infos (dec (count tag-infos)) (fn [l] (conj (vec l) attrs)))]
    (doseq [[tag tag-id tag-classes & [attrs]] tag-infos']
      (.append sb "<")
      (.append sb (name tag))
      (if attrs
        (emit-attrs sb attrs)
        (emit-attrs sb {:id tag-id :class (remove str/blank? tag-classes)}))
      (if (contains? void-tags (name tag))
        (.append sb " />")
        (.append sb ">")))
    (doseq [c children] (emit sb c opts))
    (doseq [[tag] (reverse tag-infos')]
      (when-not (contains? void-tags (name tag))
        (.append sb "</")
        (.append sb (name tag))
        (.append sb ">")))))

(defmethod emit :raw-node [^StringBuilder sb [_ {:keys [content]}] {:keys [allow-raw] :as opts}]
  (when-not allow-raw
    (throw (ex-info ":hiccup/raw-html is not allowed. Maybe you meant to set allow-raw to true?" {:content content :allow-raw allow-raw})))
  (.append sb content))

(defmethod emit :fragment-node [^StringBuilder sb [_ {:keys [children]}] opts]
  (doseq [c children]
    (emit sb c opts)))

(defmethod emit :siblings-node [^StringBuilder sb [_ {:keys [children]}] opts]
  (doseq [c children]
    (emit sb c opts)))

(defmethod emit :component-node [^StringBuilder sb [_ {:keys [view-fxn children]}] opts]
  (emit sb (parser (apply view-fxn children)) opts))

(defn html
  ([h] (html {} h))
  ([{:keys [allow-raw] :or {allow-raw false} :as _opts} h]
   (let [parsed (parser h)]
     (if (= parsed :malli.core/invalid)
       (let [{:keys [value]} (explainer h)]
         (throw (ex-info "Invalid huff form passed to html. See `huff.core/hiccup-schema` for more info" {:value value})))
       (let [sb (StringBuilder.)]
         (emit sb parsed {:allow-raw allow-raw})
         (str sb))))))

(defn page
  ([h] (page {} h))
  ([opts h] (str "<!doctype html>" (html opts h))))
