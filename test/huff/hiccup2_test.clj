;; annotated clone of hiccup2.test
(ns huff.hiccup2-test
  (:require [clojure.test :refer [deftest is testing]]
            [huff.core :as h]
            ;;[hiccup2.core :refer :all]
            ;;[hiccup.util :as util]
            ))

(deftest tag-names
  (testing "basic tags"
    ;; in huff tags are keywords only.
    (is (= "<div></div>" (h/html [:div])))

    ;; hiccup:
    ;; (is (= (str (h/html ["div"])) "<div></div>"))
    ;; (is (= (str (h/html ['div])) "<div></div>"))
    )
  (testing "tag syntax sugar"
    (is (= "<div id=\"foo\"></div>" (h/html [:div#foo])))
    (is (= "<div class=\"foo\"></div>" (h/html [:div.foo])))
    (is (= "<div class=\"foo\">barbaz</div>" (h/html [:div.foo (str "bar" "baz")])))
    (is (= "<div class=\"a b\"></div>" (h/html [:div.a.b])))
    (is (= "<div class=\"a b c\"></div>" (h/html [:div.a.b.c])))
    (is (= "<div class=\"bar baz\" id=\"foo\"></div>" (h/html [:div#foo.bar.baz])
           ;; in hiccup the id is printed last:
           ;;"<div class=\"bar baz\" id=\"foo\"></div>"
           ))))

(deftest tag-contents
  (testing "empty tags"
    (is (= "<div></div>" (h/html [:div])))
    (is (= "<h1></h1>" (h/html [:h1])))
    (is (= "<script></script>" (h/html [:script])))
    (is (= "<text></text>" (h/html [:text])))
    (is (= "<a></a>" (h/html [:a])))
    (is (= "<iframe></iframe>" (h/html [:iframe])))
    (is (= "<title></title>" (h/html [:title])))
    (is (= "<section></section>" (h/html [:section])))
    (is (= "<select></select>" (h/html [:select])))
    (is (= "<object></object>" (h/html [:object])))
    (is (= "<video></video>" (h/html [:video]))))
  (testing "void tags"
    (is (= (h/html [:br]) "<br />"))
    (is (= (h/html [:link]) "<link />"))
    (is (= (h/html [:colgroup {:span 2}]) "<colgroup span=\"2\"></colgroup>"))
    (is (= (h/html [:colgroup [:col]]) "<colgroup><col /></colgroup>")))
  (testing "tags containing text"
    (is (= (h/html [:text "Lorem Ipsum"]) "<text>Lorem Ipsum</text>")))
  (testing "contents are concatenated"
    (is (= (h/html [:body "foo" "bar"]) "<body>foobar</body>"))
    (is (= (h/html [:body [:p] [:br]]) "<body><p></p><br /></body>")))
  (testing "seqs are expanded"
    (is (= (h/html [:body (list "foo" "bar")]) "<body>foobar</body>"))
    (is (= (h/html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>")))
  ;; in hiccup, keywords are turned into strings". not in huff.
  ;; (testing "keywords are turned into strings"
  ;;   (is (= (h/html [:div :foo]) "<div>foo</div>")))
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (false? (h/valid? [:div :foo])))
    (is (false? (h/valid? [[:p "a"] [:p "b"]]))))
  (testing "tags can contain tags"
    (is (= (h/html [:div [:p]]) "<div><p></p></div>"))
    (is (= (h/html [:div [:b]]) "<div><b></b></div>"))
    (is (= (h/html [:p [:span [:a "foo"]]])
           "<p><span><a>foo</a></span></p>"))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (is (= (h/html [:xml {}]) "<xml></xml>")))
  (testing "tag with populated attribute map"
    (is (= (h/html [:xml {:a "1", :b "2"}]) "<xml a=\"1\" b=\"2\"></xml>"))
    (is (= (h/html [:img {"id" "foo"}]) "<img id=\"foo\" />"))
    (is (= (h/html [:img {'id "foo"}]) "<img id=\"foo\" />"))
    ;; (is (= (h/html [:xml {:a "1", 'b "2", "c" "3"}])
    ;;        "<xml a=\"1\" b=\"2\" c=\"3\"></xml>"))
    )
  (testing "attribute values are escaped"
    (is (= "<div id=\"&quot;\"></div>" (h/html [:div {:id "\""}]))))
  (testing "boolean attributes"
    (is (= "<input checked=\"true\" type=\"checkbox\" />"
           (h/html [:input {:type "checkbox" :checked true}])))
    (is (= "<input type=\"checkbox\" />"
           (h/html [:input {:type "checkbox" :checked false}]))))
  (testing "nil attributes"
    (is (= "<span>foo</span>"
           (h/html [:span {:class nil} "foo"]))))
  (testing "vector attributes"
    (is (= "<span class=\"bar baz\">foo</span>"
           (h/html [:span {:class ["bar" "baz"]} "foo"])))
    (is (= "<span class=\"baz\">foo</span>"
           (h/html [:span {:class ["baz"]} "foo"])))
    (is (= "<span class=\"baz bar\">foo</span>"
           (h/html [:span {:class "baz bar"} "foo"]))))
  (testing "map attributes"
    (is (= "<span style=\"background-color:blue;color:red;line-width:1.2;opacity:100%;\">foo</span>"
           (h/html [:span {:style {:background-color :blue, :color "red",
                                 :line-width 1.2, :opacity "100%"}} "foo"]))))
  (testing "resolving conflicts between attributes in the map and tag"
    (is (= (h/html [:div.foo {:class "bar"} "baz"])
           "<div class=\"bar foo\">baz</div>"))
    (is (= (h/html [:div.foo {:class ["bar"]} "baz"])
           "<div class=\"bar foo\">baz</div>"))
    (is (= (h/html [:div#bar.foo {:id "baq"} "baz"])
           "<div class=\"foo\" id=\"baq\">baz</div>"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (is (= (let [x "foo"] (h/html [:span x])) "<span>foo</span>")))
  (testing "tag content can be forms"
    (is (= (h/html [:span (str (+ 1 1))]) "<span>2</span>"))
    (is (= (h/html [:span ({:foo "bar"} :foo)]) "<span>bar</span>")))
  (testing "attributes can contain vars"
    (let [x "foo"]
      (is (= (h/html [:xml {:x x}]) "<xml x=\"foo\"></xml>"))
      (is (= (h/html [:xml {x "x"}]) "<xml foo=\"x\"></xml>"))
      (is (= (h/html [:xml {:x x} "bar"]) "<xml x=\"foo\">bar</xml>"))))
  (testing "attributes are evaluated"
    (is (= (h/html [:img {:src (str "/foo" "/bar")}])
           "<img src=\"/foo/bar\" />"))
    (is (= (h/html [:div {:id (str "a" "b")} (str "foo")])
           "<div id=\"ab\">foo</div>")))
  (testing "type hints"
    (let [string "x"]
      (is (= (h/html [:span ^String string]) "<span>x</span>"))))
  (testing "optimized forms"
    (is (= (h/html [:ul (for [n (range 3)]
                        [:li n])])
           "<ul><li>0</li><li>1</li><li>2</li></ul>"))
    (is (= (h/html [:div (if true
                         [:span "foo"]
                         [:span "bar"])])
           "<div><span>foo</span></div>")))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (h/html [:div (foo)])
      (is (= @times-called 1)))))

(deftest render-modes
  (testing "closed tag"
    ;;(is (= (h/html [:p] [:br]) "<p></p><br />"))
    (is (= (h/html [:<> [:p] [:br]]) "<p></p><br />"))
    ;; no rener modes in huff, it is for outputting xhtml.
    ;; (is (= (h/html {:mode :xhtml} [:p] [:br]) "<p></p><br />"))
    )
  (testing "laziness and binding scope"
    (is (= (h/html [:html [:link] (list [:link])])
           "<html><link /><link /></html>")))
  (testing "function binding scope"
    (let [f #(vector :p "<>" [:br])]
      (is (= (h/html [f]) "<p>&lt;&gt;<br /></p>"))
      (is (= (h/html (f)) "<p>&lt;&gt;<br /></p>")))))

(deftest auto-escaping
  (testing "literals"
    (is (= (h/html "<>") "&lt;&gt;"))
    ;; invalid in huff
    (is (false? (h/valid? :<>)))
    (is (= (h/html ^String (str "<>")) "&lt;&gt;"))
    (is (false? (h/valid? {"<a>" "<b>"})))
    (is (false? (h/valid? #{"<>"})))
    (is (= (h/html 1) "1"))
    (is (= (h/html ^Number (+ 1 1)) "2")))
  (testing "non-literals"
    (is (= (h/html (list [:p "<foo>"] [:p "<bar>"]))
           "<p>&lt;foo&gt;</p><p>&lt;bar&gt;</p>"))
    (is (= (h/html ((constantly "<foo>"))) "&lt;foo&gt;"))
    (is (= (let [x "<foo>"] (h/html x)) "&lt;foo&gt;")))
  (testing "elements"
    (is (= (h/html [:p "<>"]) "<p>&lt;&gt;</p>"))
    (is (= (h/html [:p {:class "<\">"}])
           "<p class=\"&lt;&quot;&gt;\"></p>"))
    (is (= (h/html [:p {:class ["<\">"]}])
           "<p class=\"&lt;&quot;&gt;\"></p>"))
    (is (= (h/html [:ul [:li "<foo>"]])
           "<ul><li>&lt;foo&gt;</li></ul>")))
  (testing "raw strings"
    (is (= (h/html [:hiccup/raw-html "<foo>"]) "<foo>"))
    (is (= (h/html [:p [:hiccup/raw-html "<foo>"]]) "<p><foo></p>"))
    (is (= (h/html [:ul [:li "<>"]]) "<ul><li>&lt;&gt;</li></ul>"))))

;; in huff do not call the compiler multiple times on the same value,
;; instead use the `:hiccup/raw-html` tag

(deftest html-escaping
  (testing "precompilation"
    (is (= (h/html [:p "<>"]) "<p>&lt;&gt;</p>"))
    (is (= (binding [h/*escape?* false]
             (h/html [:p "<>"])) "<p><></p>")))
  (testing "dynamic generation"
    (let [x [:p "<>"]]
      (is (= (h/html x) "<p>&lt;&gt;</p>"))))
  (testing "attributes"
    (is (= (h/html [:p {:class "<>"}]) "<p class=\"&lt;&gt;\"></p>"))
    (is (= (binding [h/*escape?* false]
             (h/html [:p {:class "<>"}]))
           "<p class=\"<>\"></p>")))
  (testing "raw strings"
    (is (= (h/html [:p [:hiccup/raw-html "<>"]]) "<p><></p>"))
    (is (= (binding [h/*escape?* false] (h/html [:p [:hiccup/raw-html "<>"]]))
           "<p><></p>"))))
