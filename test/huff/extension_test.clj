(ns huff.extension-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [huff2.extension :as h2e]
   [huff2.core :as h]))

(deftest simplest-extension
  (let [my-schema (h2e/add-schema-branch h/hiccup-schema :my/child-counter-tag)
        ;; emit gets passed: [:my/doubler-tag [:my/doubler-tag ["one" "two" "three"]]]
        _add-emitter (defmethod h/emit :my/child-counter-tag [append! [_ [_ values]] opts]
                       (is (= values ["one" "two" "three"]))
                       (append! "I have " (count values) " children."))
        my-html (partial h/html (h2e/custom-fxns! my-schema))]
    (is (= "<div><h1>I have 3 children.</h1></div>"
           (str (my-html [:div>h1 [:my/child-counter-tag "one" "two" "three"]]))))))

(deftest catn-named-values-in-schema
  (let [my-schema (h2e/add-schema-branch
                    h/hiccup-schema
                    :my/doubler-tag
                    [:catn
                     [:tag [:= :my/doubler-tag]]
                     [:number :int]])
        ;; emit gets passed: [:my/doubler-tag {:tag :my/doubler-tag, :number 3}]
        _add-emitter (defmethod h/emit :my/doubler-tag [append!
                                                        [_ {:keys [number] :as arg}]
                                                        opts]
                       (is (= {:tag :my/doubler-tag, :number 3} arg))
                       (append! (* 2 number)))
        custom-fxns (h2e/custom-fxns! my-schema)
        my-html (partial h/html custom-fxns)]
    ;; it knows what is not allowed:
    (is (= :malli.core/invalid ((:*parser custom-fxns) [:my/doubler-tag "G"])))
    (is (= "<div><h1>6</h1></div>" (str (my-html [:div>h1 [:my/doubler-tag 3]]))))))

(deftest adding-both-of-them
  (let [my-schema (-> h/hiccup-schema
                      (h2e/add-schema-branch :my/child-counter-tag)
                      (h2e/add-schema-branch :my/doubler-tag [:catn
                                                              [:tag [:= :my/doubler-tag]]
                                                              [:number :int]]))
        _add-emitter (defmethod h/emit :my/child-counter-tag
                       [append! [_ [_ values]] opts]
                       (append! "I have " (count values) " children."))
        _add-emitter (defmethod h/emit :my/doubler-tag
                       [append! [_ {:keys [number]}] opts]
                       (append! (* 2 number)))
        my-html (partial h/html (h2e/custom-fxns! my-schema))]
    (is (= "<div><div><h1>I have 4 children.</h1></div><div><h1>20</h1></div><div>I have 3 children.</div></div>"
           (str (my-html
                  [:div
                   [:>h1 [:my/child-counter-tag "one" "two" "three" "four"]]
                   [:>h1 [:my/doubler-tag 10]]
                   [(keyword "") [:my/child-counter-tag
                                  [:my/doubler-tag 3]
                                  [:my/doubler-tag 3]
                                  [:my/doubler-tag 3]]]]))))))

(deftest extensions-apply-to-component-nodes
  (let [my-schema (h2e/add-schema-branch h/hiccup-schema :my/child-counter-tag)
        _add-emitter (defmethod h/emit :my/child-counter-tag [append! [_ [_ values]] opts]
                       (is (= values ["one" "two" "three"]))
                       (append! "I have " (count values) " children."))
        my-html (partial h/html (h2e/custom-fxns! my-schema))]
    (testing "a component node that has hiccup that is only valid with the extension"
      (is (=
            "<div><h1>I have 3 children.</h1></div>"
            (str (my-html [(constantly [:div>h1 [:my/child-counter-tag "one" "two" "three"]])])))))))
