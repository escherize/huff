(ns huff.core-test
  (:require [huff.core :as h]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(deftest class-list-equality-test
  (is (= (h/html [:div.c1#id1.c2 {:class ["c3"] :style {:border "1px solid red"}} "x"])
         (h/html [:div.c1#id1.c2 {:class "c3" :style {:border "1px solid red"}} "x"]))))

(deftest simple-html-test
  (is (= "<div class=\"x\">y</div>" (h/html [:div {:class "x"} "y"])))

  (is "<div></div>" (h/html [:div]))

  (is (= "<div>x</div>"
         (h/html [:div "x"])))

  (is (= (h/html [:div {:style {:color "red" :background "black"}} "x" [:div {:style {:padding "10px"}} "z"] "y"])
         "<div style=\"background:black;color:red;\">x<div style=\"padding:10px;\">z</div>y</div>"))

  (is (= (h/html [:div.class#id])
         "<div class=\"class\" id=\"id\"></div>"))

  (is (true?
       (= (h/html [:div [:<> [:<> [:<> "x"]]]])
          (h/html [:div [:<> [:<> "x"]]])
          (h/html [:div [:<> "x"]]))))

  (is (= (h/html [:h1 {:style {:border "1px solid red"}} "k"])
         "<h1 style=\"border:1px solid red;\">k</h1>"))

  (is (= (h/html [:p "raw x"])
         (str "<p>" (h/html [:hiccup/raw-html "raw x"]) "</p>")))
  true

  (is (= (h/html [:ul (for [n (range 3)] [:li n])])
         "<ul><li>0</li><li>1</li><li>2</li></ul>"))

  (is (= (let [double (fn double [hic] [:<> hic hic])
               pre (fn pre [& xs]
                     [:div
                      [:p "hello."]
                      [:<>
                       [:ul (for [x (range 3 5)] [:li x])]
                       [:pre "pre says: " (str/join "|" xs)]]])]
           (h/html [:p [double [pre 1 2]]]))
         "<p><div><p>hello.</p><ul><li>3</li><li>4</li></ul><pre>pre says: 1|2</pre></div><div><p>hello.</p><ul><li>3</li><li>4</li></ul><pre>pre says: 1|2</pre></div></p>")))

(deftest style-test
  (is (= "<div style=\"border:1px solid red;color:red;\">x</div>"
        (h/html [:div {:style {:color "red" :border "1px solid red"}} "x"])))
  (is (= "<div style=\"border:1px solid red;\">x</div>"
        (h/html [:div {:style {:border "1px solid red"}} "x"])))
  (is (= "<div style=\"border: 1px solid red\">x</div>"
        (h/html [:div {:style "border: 1px solid red"} "x"]))))

(deftest attr-emission-test
  (is (= " class=\"x y\" id=\"x\"" (#'h/emit-attrs {:id "x" :class ["x" "y"]})))
  (is (= (#'h/emit-attrs {:id "x" :class []}) " id=\"x\"")))

(deftest page-test
  (is (= (h/page [:h1 "hi"]) "<!doctype html><h1>hi</h1>")))

(deftest escape-test
  (is (= "<div>&quot;</div>" (h/html [:div "\""]))))


(deftest unescape-test
  (is (= "<div>\"</div>" (binding [h/*escape?* false]
                           (h/html [:div "\""])))))
