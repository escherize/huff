(ns huff.core2-test
  (:require [huff2.core :as h]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(deftest class-list-equality-test
  (is (= (h/html [:div.c1#id1.c2 {:class ["c3"] :style {:border "1px solid red"}} "x"])
         (h/html [:div.c1#id1.c2 {:class "c3" :style {:border "1px solid red"}} "x"])))
  (is (= (h/html [:div {:class nil}]) (h/html [:div])))
  (is (= (h/html [:div.a]) (h/html [:div {:class ["a" nil "" ""]}]))))

(defn as-string [f]
  (let [sb (StringBuilder.)
        append! (fn append! [& strings]
                  (doseq [s strings
                          :when s] (.append ^StringBuilder sb s)))]
    (f append!)
    (str sb)))

(deftest simple-html-test
  (is (= "<div class=\"x\">y</div>" (str (h/html [:div {:class "x"} "y"]))))

  (is (= "<div class=\"x g\">y</div>" (str (h/html [:div.g {:class "x"} "y"]))))

  (is (=  "<div class=\"parent\"><div class=\"me-go-onto-child child\" id=\"me-too\">y</div></div>"
          (str (h/html [:.parent>div.child {:class "me-go-onto-child" :id "me-too"} "y"]))))

  (is (= "<div></div>" (str (h/html [:div]))))

  (is (= "<div>x</div>"
         (str (h/html [:div "x"]))))

  (is (str (h/html [:div {:style {:color "red" :background "black"}} "x" [:div {:style {:padding "10px"}} "z"] "y"])
         (= "<div style=\"background:black;color:red;\">x<div style=\"padding:10px;\">z</div>y</div>")))

  (is (= "<div id=\"id\" class=\"class\"></div>"
         (str (h/html [:div.class#id]))))

  (is (true?
        (= (str (h/html [:div [:<> [:<> [:<> "x"]]]]))
           (str (h/html [:div [:<> [:<> "x"]]]))
           (str (h/html [:div [:<> "x"]])))))

  (is (= "<h1 style=\"border:1px solid red;\">k</h1>"
         (str (h/html [:h1 {:style {:border "1px solid red"}} "k"]))))

  (is (= (str (h/html [:p "raw x"]))
         (str "<p>" (h/html {:allow-raw true}
                            [:hiccup/raw-html "raw x"]) "</p>")))

  (is (= "<ul><li>0</li><li>1</li><li>2</li></ul>"
         (str (h/html [:ul (for [n (range 3)] [:li n])]))))

  (is (= "<p><div><p>hello.</p><ul><li>3</li><li>4</li></ul><pre>pre says: 1|2</pre></div><div><p>hello.</p><ul><li>3</li><li>4</li></ul><pre>pre says: 1|2</pre></div></p>"
         (let [double (fn double [hic] [:<> hic hic])
               pre (fn pre [& xs]
                     [:div
                      [:p "hello."]
                      [:<>
                       [:ul (for [x (range 3 5)] [:li x])]
                       [:pre "pre says: " (str/join "|" xs)]]])]
           (str (h/html [:p [double [pre 1 2]]]))))))

(deftest style-test

  (is (= "<div style=\"border:1px solid red;color:red;\">x</div>"
         (str (h/html [:div {:style {:color "red" :border "1px solid red"}} "x"]))))
  (is (= "<div style=\"border:1px solid red;\">x</div>"
         (str (h/html [:div {:style {:border "1px solid red"}} "x"]))))
  (is (= "<div style=\"border: 1px solid red\">x</div>"
         (str (h/html [:div {:style "border: 1px solid red"} "x"])))))


(deftest attr-emission-test
  (is (= " id=\"x\" class=\"x y\"" (as-string #(#'h/emit-attrs % {:id "x" :class ["x" "y"]}))))
  (is (= " id=\"x\" class=\"x y\"" (as-string #(#'h/emit-attrs % {:id "x" :class "x y"}))))
  (is (= " id=\"x\"" (as-string #(#'h/emit-attrs % {:id "x" :class []}))))
  (is (= " x-data=\"{open: false}\"" (as-string #(#'h/emit-attrs % {:x-data "{open: false}"})))))

(deftest page-test
  (is (= (h/page {:allow-raw true} [:h1 "hi"]) "<!doctype html><h1>hi</h1>")))

(deftest escape-test
  (is (= "<div>&amp;</div>"  (str (h/html [:div "&"]))))
  (is (= "<div>&lt;</div>"   (str (h/html [:div "<"]))))
  (is (= "<div>&gt;</div>"   (str (h/html [:div ">"]))))
  (is (= "<div>&quot;</div>" (str (h/html [:div "\""]))))
  (is (= "<div>&#39;</div>"  (str (h/html [:div "\\"]))))
  (is (= "<div>&amp;</div><div>&lt;</div><div>&gt;</div><div>&quot;</div><div>&#39;</div>"
         (str (h/html [:<>
                       [:div "&"]
                       [:div "<"]
                       [:div ">"]
                       [:div "\""]
                       [:div "\\"]])))))

(deftest unescape-test
  (is (= "<div>\"</div>"
         (binding [h/*escape?* false] (str (h/html [:div "\""]))))))

(deftest style-attr-args-test
  (is (= "<div style=\"background-color:red;\"></div>"
         (str (h/html [:div {:style {:background-color "red"}}])))))

(deftest style-attr-numeric-values-append-px-test
  (is (= (str (h/html [:div {:style {:width (h/px (* 5 2))}}]))
         "<div style=\"width:10px;\"></div>"))
  (is (= (str (h/html [:div {:style {:height (h/px (* 50 2))}}]))
         "<div style=\"height:100px;\"></div>")))

(deftest do-not-pxify-every-attr-test
  (is (= "<h1 style=\"opacity:1;width:20px;\"></h1>"
         (str (h/html [:h1 {:style {:width (-> 20 h/px) :opacity 1}}])))))

(deftest dot-shortcut-for-div-test
  (is (= (#'h/tag->tag+id+classes :.)
         (#'h/tag->tag+id+classes :div))))

(deftest lists-work-for-spreading
  (is (= "<div><span>ok</span></div>" (str (h/html [:div '([:span "ok"])]))))
  (is (= "<div></div>" (str (h/html [:<> '([:div])])))))

(deftest html-raw-disallowed-test
  (is (= {:content "<script>mine bitcoin</script>", :allow-raw false}
         (try (str (h/html [:hiccup/raw-html "<script>mine bitcoin</script>"]))
              (catch Exception e (ex-data e)))))

  (is (= "<script>mine bitcoin</script>"
         (str (h/html {:allow-raw true}
                      [:hiccup/raw-html "<script>mine bitcoin</script>"])))))

(deftest page-raw-allowed-test
  (is (= {:content "hi", :allow-raw false}
         (try (h/page [:div [:hiccup/raw-html "hi"]])
              (catch Exception e (ex-data e)))))

  (is (= "<!doctype html><h1>1</h1>"
         (h/page {:allow-raw true} [:h1 1]))))

(deftest secure-raw-html
  (is (= "<!doctype html><div>select * from students where name = 'Bob Robert'); DROP TABLE Students;--</div>"
         (h/page [:div
                  "select * from students where name = 'Bob "
                  [:hiccup/raw-html (h/raw "Robert'); DROP TABLE Students;--")]]))))

(deftest you-can-invalidate-your-own-html
  (is (= "<!doctype html><div>My Page<ul><li><!-- hehe...</li><li>what happened...</li></ul></div>"
         (h/page [:div "My Page"
                  [:ul
                   [:li [:hiccup/raw-html (h/raw "<!-- hehe...")]]
                   [:li "what happened..."]]]))))

(deftest users-cannot-invalidate-your-own-html-test
  (is (= {:content "<!-- hehe...", :allow-raw false}
         (try (h/page [:div "My Page"
                       [:ul
                        [:li [:hiccup/raw-html "<!-- hehe..."]]
                        [:li "what happened..."]]])
              (catch Exception e (ex-data e))))))

(deftest compiled-huff-is-a-raw-string-primative
  (is (= "<div><div>hi</div></div>"
         (str (h/html [:div (h/html [:div "hi"])])))))

(deftest styles-with-convenience-functions-test
  (is (= "<div style=\"width:10px;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/px)}}]))))
  (is (= "<div style=\"width:10em;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/em)}}]))))
  (is (= "<div style=\"width:10rem;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/rem)}}]))))
  (is (= "<div style=\"width:10%;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/pct)}}]))))
  (is (= "<div style=\"width:10vw;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/vw)}}]))))
  (is (= "<div style=\"width:10vh;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/vh)}}]))))
  (is (= "<div style=\"width:10vmin;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/vmin)}}]))))
  (is (= "<div style=\"width:10vmax;\"></div>"
         (str (h/html [:div {:style {:width (-> 10 h/vmax)}}])))))
