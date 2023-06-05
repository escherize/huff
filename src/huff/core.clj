(ns huff.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [malli.core :as m]))

(def hiccup-schema
  [:schema
   {:registry
    {"primative" [:or :string number? :boolean :nil]
     "hiccup"    [:orn
                  [:fragment-node  [:catn
                                    [:fragment-indicator [:= :<>]]
                                    [:children           [:* [:schema [:ref "hiccup"]]]]]]
                  ;;[:hiccup-coll [:sequential [:schema [:ref "hiccup"]]]]
                  [:tag-node       [:catn
                                    [:tag      simple-keyword?]
                                    [:props    [:? [:map-of :keyword :any]]]
                                    [:children [:* [:schema [:ref "hiccup"]]]]]]
                  [:raw-node       [:catn
                                    [:raw     [:= :hiccup/raw-html]]
                                    [:content :string]]]
                  [:component-node [:catn [:view-fxn [:function [:=>
                                                                 [:cat :any]
                                                                 [:schema [:ref "hiccup"]]]]]
                                    [:children [:* :any]]]]
                  [:primitive      [:ref "primative"]]]}}
   "hiccup"])

(def ^:private parse (m/parser hiccup-schema))

(defmulti ^:private emit
  #_(fn [& xs] (prn ["emit" xs]) (first xs))
  first)

(defmethod emit :primitive [[_ x]] x)

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
         (map? s) (str/join ";" (for [[k v] s] (str (name k) ": " v)))
         (string? s) s)
       "\""))

(defn emit-props [p]
  (let [outs (keep (fn [[k value]]
                     (when (not-empty value)
                       (cond
                         (= :style k) (emit-style value)
                         (string? value) (str (name k) "=\"" value "\"")
                         (coll? value)  (str (name k) "=\"" (str/join " " value) "\""))))
                   p)]
    (str (when (seq outs) " ") (str/join " " outs))))

(defmethod emit :tag-node [[_ {:keys [tag props children] :as in}]]
  (let [[tag tag-id tag-classes] (tag->tag+id+classes tag)
        props (-> props
                  (update :id #(or % tag-id))
                  (update :class #(cond
                                    (string? %) (concat [%] tag-classes)
                                    (coll? %) (concat % tag-classes)
                                    (nil? %) tag-classes)))]
    (str "<" (name tag) (emit-props props) ">"
         (str/join (map emit children))
         (str "</" (name tag) ">"))))

(defmethod emit :raw-node [[_ {:keys [content]}]]
  content)

(defmethod emit :fragment-node [[_ {:keys [children]}]]
  (str/join "" (mapv #(emit %) children)))

(defmethod emit :hiccup-coll [[_ {:keys [children]}]]
  (str/join "" (mapv emit children)))

(defn- list->fragment [x]
  (walk/postwalk
    (fn [x]
      (cond->> x
        (#{clojure.lang.PersistentList clojure.lang.LazySeq} (type x)) (into [:<>])))
    x))

(defn html [h] (-> h list->fragment parse emit))

(defmethod emit :component-node [[_ {:keys [view-fxn children]}]]
  (html (apply view-fxn children)))

(defn page [h] (html [:<> [:hiccup/raw-html "<!doctype html>"]
                      h]))
