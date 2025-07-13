(ns huff2.extension
  (:require
   [clojure.walk :as walk]
   [huff2.core :as h]
   [malli.core :as m]))

(defn custom-fxns! [hiccup-schema]
  {:*explainer (m/explainer hiccup-schema)
   :*parser (m/parser hiccup-schema)})

(defn- hiccup-branches? [element]
  (and (vector? element)
       (= :orn (first element))
       (-> element second ::h/branches)))

(defn add-schema-branch
  "[[tag-schema]] dictates what functions will be passed to the [[huff2.core/emit]] function which you must supply."
  ([hiccup-schema node-name]
   (add-schema-branch hiccup-schema node-name
                      ;; This is the simplest form that a branch should have:
                      ;; the arg vector for [[huff2.core/emit]] should then look like:
                      ;; [append! [_tag [_tag values]]]
                      [:cat [:= node-name] [:* :any]]))
  ([hiccup-schema node-name node-schema]
   (walk/postwalk
     (fn [element]
       ;; We want to add a branch to the hiccup branches.
       (if (hiccup-branches? element)
         (vec (concat
                (take 2 element)
                [[node-name node-schema]]
                (drop 2 element)))
         element))
     hiccup-schema)))

(comment

  ;; Simplest example:

  (def my-schema
    (add-schema-branch h/hiccup-schema :my/child-counter-tag))

  ;;-(defmethod emit :fragment-node [append! [_ {:keys [children]}] opts]
;;+(defmethod emit :fragment-node [append! {{{:keys [children]} :values} :value} opts]

  (defmethod h/emit :my/child-counter-tag [append! {{:keys [values]} :value} _opts]
    (append! "I have " (count values) " children."))

  ;; Call your function, integrated with hiccup:

  (h/html
    (custom-fxns! my-schema)
    [:div>h1 [:my/child-counter-tag "one" "two" "three"]])
    ;; => "<div><h1>I have 3 children.</h1></div>"


  )
