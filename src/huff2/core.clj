(ns huff2.core
  (:refer-clojure :exclude [rem])
  (:require
   [clojure.core :exclude [rem]]
   [clojure.string :as str]
   [malli.core :as m]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; convenience functions

(defn px "Convenience function to append 'px'" [x] (str x "px"))
(defn em "Convenience function to append 'em'" [x] (str x "em"))
(defn rem "Convenience function to append 'rem'" [x] (str x "rem"))
(defn pct "Convenience function to append '%'" [x] (str x "%"))
(defn vw "Convenience function to append 'vw'" [x] (str x "vw"))
(defn vh "Convenience function to append 'vh'" [x] (str x "vh"))
(defn vmin "Convenience function to append 'vmin'" [x] (str x "vmin"))
(defn vmax "Convenience function to append 'vmax'" [x] (str x "vmax"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; raw strings

;; inspired by hiccup.util
(deftype RawString [^String s]
  Object
  (^String toString [_this] s)
  (^boolean equals [_this other]
   (and (instance? RawString other)
        (= s (str other)))))

(defn raw-string
  "Converts one or more strings into an object that will not be escaped when
  used with the [[huff2.core/html]] function."
  {:arglists '([& strs])}
  ([] (RawString. ""))
  ([s] (RawString. (str s)))
  ([s & strs] (RawString. (apply str s strs))))

(def ^{:arglists '([& strs])} raw raw-string)

(defn raw-string?
  "Returns true if x is a RawString created by [[raw-string]]."
  [x]
  (instance? RawString x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Schema

(def hiccup-schema
  [:schema
   {:registry
    {"hiccup" [:orn {::branches true}
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
                                 [:content [:orn
                                            [:raw-string [:fn raw-string?]]
                                            [:string :string]]]]]
               [:component-node [:catn
                                 [:view-fxn [:and
                                             [:function [:=> [:cat :any]
                                                         [:schema [:ref "hiccup"]]]]
                                             [:not keyword?]
                                             [:not vector?]]]
                                 [:children [:* :any]]]]
               [:primative [:or
                            :string
                            number?
                            :boolean
                            :nil
                            [:fn raw-string?]]]]}}
   "hiccup"])

(let [validator (m/validator hiccup-schema)]
  (defn valid? [h] (validator h)))

(def explainer (m/explainer hiccup-schema))
(def parser (m/parser hiccup-schema))

(defn stringify
  "Take a primative, and turn it into a string."
  [text]
  (cond
    (nil? text) ""
    (simple-keyword? text) (name text)
    (keyword? text) [(namespace text) "/" (name text)]
    (ratio? text) (str (double text))
    (raw-string? text) (str text)
    :else (str text)))

(def ^:dynamic *escape?* true)

(def ^:private char->replacement {\& "&amp;", \< "&lt;", \> "&gt;", \" "&quot;", \\ "&#39;"})

(defn maybe-escape-html
  "1. Change special characters into HTML character entities when *escape?*
   2. call `append!` on the maybe-transformed text value"
  [append! text]
  (let [text-str (stringify text)]
    (if (or (not *escape?*) (raw-string? text))
      (append! text-str)
      (let [some-replacement? (some char->replacement text-str)]
        (if some-replacement?
          (let [s (into [] text-str)]
            (doseq [c s] (append! (char->replacement c c))))
          (append! text-str))))))

(defmulti emit
  (fn [_append! form _opts] (:key form)))

(defmethod emit :primative [append! {:keys [value]} _opts]
  (maybe-escape-html append! value))

(defn- empty-or-div [seen] (if (empty? seen) "div" (str/join seen)))

(defn- emit-style [append! s]
  (append! "style=\"")
  (cond
    (map? s) (doseq [[k v] (sort-by first s)]
               (append! (stringify k) ":" (stringify v) ";"))
    (string? s) (append! s)
    :else (throw (ex-info "style attributes need to be a string or a map." {:s s})))
  (append! "\""))

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

(defn- emit-attrs [append! attrs]
  (doseq [[k value] attrs]
    (when-not
        (or (contains? #{"" nil false} value)
            (and (coll? value) (empty? value)))
      (append! " ")
      (cond
        (= :style k)
        (emit-style append! value)

        (coll? value)
        (do (append! (stringify k) "=\"")
            (doseq [x (interpose " " value)] (maybe-escape-html append! x))
            (append! "\""))

        :else
        (do (append! (stringify k) "=\"")
            (maybe-escape-html append! value)
            (append! "\""))))))

;; lifted from hiccup.compiler
(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen"
  "link" "meta" "param" "source" "track" "wbr"})

(defmethod emit :tag-node-no-attrs [append! {{{:keys [tag children]} :values} :value} opts]
  (let [tag-infos (tag->tag+id+classes tag)]
    ;; emit opening tags:
    (doseq [[tag tag-id tag-classes] tag-infos]
      (let [tag-classes' (remove str/blank? tag-classes)]
        (append! "<")
        (append! ^String (name tag))
        (when (or tag-id (not-empty tag-classes'))
          (emit-attrs append! {:id tag-id :class tag-classes'}))
        (if (contains? void-tags (name tag))
          (append! " />")
          (append! ">"))))
    ;;children
    (doseq [c children] (emit append! c opts))
    ;;closing tags
    (doseq [[tag] (reverse tag-infos)]
      (when-not (contains? void-tags (name tag))
        (append! "</" (name tag) ">")))))

(defmethod emit :tag-node [append! {{{:keys [tag attrs children]} :values} :value} opts]
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
      (append! "<")
      (append! ^String (name tag))
      (if attrs
        (emit-attrs append! attrs)
        (emit-attrs append! {:id tag-id :class (remove str/blank? tag-classes)}))
      (if (contains? void-tags (name tag))
        (append! " />")
        (append! ">")))
    (doseq [c children] (emit append! c opts))
    (doseq [[tag] (reverse tag-infos')]
      (when-not (contains? void-tags (name tag))
        (append! "</")
        (append! ^String (name tag))
        (append! ">")))))

(defmethod emit :raw-node [append!
                           {{{{:keys [key value]} :content } :values} :value}
                           {:keys [allow-raw]}]
  ;; This either gets a string, in which case you need the allow-raw option set,
  ;; or a "raw-string" which is a wrapper for a string that cannot be
  ;; constructed outside of your program.
  (case key
    :string
    (if allow-raw
      (append! value)
      (throw (ex-info ":hiccup/raw-html is not allowed. Maybe you meant to set allow-raw to true?"
                      {:content value :allow-raw allow-raw})))
    :raw-string
    (append! (str value))))

(defmethod emit :fragment-node [append! {{{:keys [children]} :values} :value} opts]
  (doseq [c children]
    (emit append! c opts)))

(defmethod emit :siblings-node [append! {{{:keys [children]} :values} :value} opts]
  (doseq [c children]
    (emit append! c opts)))

(defmethod emit :component-node [append! {{{:keys [view-fxn children]} :values} :value} {:keys [parser] :as opts}]
  (emit append! (parser (apply view-fxn children)) opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Public api

(defn html
  "Generates html from hiccupy data-structures.

  What's hiccupy data look like? see: [[hiccup-schema]].

  Can I extend this by adding new types of nodes? Yes: see: [[huff2.extension]]!"
  ([h] (html {} h))
  ([{:keys [allow-raw *explainer *parser] :or {allow-raw false
                                               *explainer explainer
                                               *parser parser} :as _opts} h]
   (let [parsed (*parser h)]
     (if (= parsed :malli.core/invalid)
       (let [{:keys [value]} (*explainer h)]
         (throw (ex-info "Invalid huff form passed to html. See [[hiccup-schema]] for more info" {:value value})))
       (let [sb (StringBuilder.)
             append! (fn append! [& strings] (doseq [s strings :when s] (.append ^StringBuilder sb s)))]
         (emit append! parsed {:allow-raw allow-raw :parser *parser})
         (raw-string (str sb)))))))

(defn page
  ([h] (page {} h))
  ([opts h] (str "<!doctype html>" (html opts h))))
