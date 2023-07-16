(ns huff.core-test
  (:require [huff.core :as h]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(deftest class-list-equality-test
  (is (= (h/html [:div.c1#id1.c2 {:class ["c3"] :style {:border "1px solid red"}} "x"])
         (h/html [:div.c1#id1.c2 {:class "c3" :style {:border "1px solid red"}} "x"])))
  (is (= (h/html [:div {:class nil}]) (h/html [:div])))
  (is (= (h/html [:div.a]) (h/html [:div {:class ["a" nil "" ""]}]))))

(deftest simple-html-test
  (is (= "<div class=\"x\">y</div>" (h/html [:div {:class "x"} "y"])))

  (is (= "<div class=\"x g\">y</div>" (h/html [:div.g {:class "x"} "y"])))

  (is (=  "<div class=\"parent\"><div class=\"me-go-onto-child child\" id=\"me-too\">y</div></div>"
          (h/html [:.parent>div.child {:class "me-go-onto-child" :id "me-too"} "y"])))

  (is "<div></div>" (h/html [:div]))

  (is (= "<div>x</div>"
         (h/html [:div "x"])))

  (is (= (h/html [:div {:style {:color "red" :background "black"}} "x" [:div {:style {:padding "10px"}} "z"] "y"])
         "<div style=\"background:black;color:red;\">x<div style=\"padding:10px;\">z</div>y</div>"))

  (is (= "<div id=\"id\" class=\"class\"></div>"
         (h/html [:div.class#id])))

  (is (true?
       (= (h/html [:div [:<> [:<> [:<> "x"]]]])
          (h/html [:div [:<> [:<> "x"]]])
          (h/html [:div [:<> "x"]]))))

  (is (= (h/html [:h1 {:style {:border "1px solid red"}} "k"])
         "<h1 style=\"border:1px solid red;\">k</h1>"))

  (is (= (h/html [:p "raw x"])
         (str "<p>" (h/html {:allow-raw true}
                            [:hiccup/raw-html "raw x"]) "</p>")))

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

(defn as-string [f]
  (let [sb (StringBuilder.)] (f sb) (str sb)))

(deftest attr-emission-test
  (is (= " id=\"x\" class=\"x y\"" (as-string #(#'h/emit-attrs % {:id "x" :class ["x" "y"]}))))
  (is (= " id=\"x\" class=\"x y\"" (as-string #(#'h/emit-attrs % {:id "x" :class "x y"}))))
  (is (= " id=\"x\"" (as-string #(#'h/emit-attrs % {:id "x" :class []}))))
  (is (= " x-data=\"{open: false}\"" (as-string #(#'h/emit-attrs % {:x-data "{open: false}"})))))

(deftest page-test
  (is (= (h/page {:allow-raw true} [:h1 "hi"]) "<!doctype html><h1>hi</h1>")))

(deftest escape-test
  (is (= "<div>&amp;</div>"  (h/html [:div "&"])))
  (is (= "<div>&lt;</div>"   (h/html [:div "<"])))
  (is (= "<div>&gt;</div>"   (h/html [:div ">"])))
  (is (= "<div>&quot;</div>" (h/html [:div "\""])))
  (is (= "<div>&#39;</div>"  (h/html [:div "\\"])))
  (is (= "<div>&amp;</div><div>&lt;</div><div>&gt;</div><div>&quot;</div><div>&#39;</div>"
         (h/html [:<>
                  [:div "&"]
                  [:div "<"]
                  [:div ">"]
                  [:div "\""]
                  [:div "\\"]]))))

(deftest unescape-test
  (is (= "<div>\"</div>"
         (binding [h/*escape?* false] (h/html [:div "\""])))))

(deftest style-attr-args-test
  (is (= "<div style=\"background-color:red;\"></div>"
         (h/html [:div {:style {:background-color "red"}}]))))


(deftest style-attr-numeric-values-append-px
  (is (= (h/html [:div {:style {:width (* 5 2)}}])
         "<div style=\"width:10px;\"></div>"))
  (is (= (h/html [:div {:style {:height (* 50 2)}}])
         "<div style=\"height:100px;\"></div>")))

(deftest dot-shortcut-for-div
  (is (= (#'h/tag->tag+id+classes :.) (#'h/tag->tag+id+classes :div))))

(deftest lists-work-for-spreading
  (is (= "<div><span>ok</span></div>" (h/html [:div '([:span "ok"])])))
  (is (= "<div></div>" (h/html [:<> '([:div])]))))

(deftest html-raw-disallowed-test
  (is (= {:content "<script>mine bitcoin</script>", :allow-raw false}
         (try (h/html [:hiccup/raw-html "<script>mine bitcoin</script>"])
              (catch Exception e (ex-data e)))))

  (is (= "<script>mine bitcoin</script>"
         (h/html {:allow-raw true}
                 [:hiccup/raw-html "<script>mine bitcoin</script>"]))))

(deftest page-raw-allowed-test
  (is (= {:content "hi", :allow-raw false}
         (try (h/page [:div [:hiccup/raw-html "hi"]])
              (catch Exception e (ex-data e)))))

  (is (= "<!doctype html><h1>1</h1>"
         (h/page {:allow-raw true} [:h1 1]))))
