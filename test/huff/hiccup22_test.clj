;; annotated clone of hiccup2.test
(ns huff.hiccup22-test
  (:require [clojure.test :refer [deftest are is testing]]
            [huff2.core :as h]
            ;;[hiccup2.core :refer :all]
            ;;[hiccup.util :as util]
            ))

(deftest kw-tag-parsing
  (are [x y] (= (#'h/tag->tag+id+classes x) y)
    :div       [["div" nil []]]
    :div.a     [["div" nil ["a"]]]
    :div.a#d   [["div" "d" ["a"]]]
    :div#d.a   [["div" "d" ["a"]]]
    :div.a.b   [["div" nil ["a" "b"]]]
    :div#d.a.b [["div" "d" ["a" "b"]]]
    :div.a#d.b [["div" "d" ["a" "b"]]]
    :div.a.b#d [["div" "d" ["a" "b"]]]
    :div#d.b.a [["div" "d" ["b" "a"]]]
    :div.b#d.a [["div" "d" ["b" "a"]]]
    :div.a.a#d [["div" "d" ["a" "a"]]]
    :.>        [["div" nil []]]
    :div       [["div" nil []]]
    :.>.       [["div" nil []] ["div" nil []]]

    ;; . shorthand for div:
    :.>.>.       [["div" nil []] ["div" nil []] ["div" nil []]]
    :div>div>div [["div" nil []] ["div" nil []] ["div" nil []]]
    :.>div>div   [["div" nil []] ["div" nil []] ["div" nil []]]
    :div>.>div   [["div" nil []] ["div" nil []] ["div" nil []]]
    :div>div>.   [["div" nil []] ["div" nil []] ["div" nil []]]
    :.>.>div     [["div" nil []] ["div" nil []] ["div" nil []]]
    :.>div>.     [["div" nil []] ["div" nil []] ["div" nil []]]
    :div>.>.     [["div" nil []] ["div" nil []] ["div" nil []]]
    :>>div       [["div" nil []] ["div" nil []] ["div" nil []]]

    ;; crazy stuff
    :.a#a>.b#b>.c#c              [["div" "a" ["a"]] ["div" "b" ["b"]] ["div" "c" ["c"]]]
    :>>>>>>>>>>>>>>>>>>>>>>>>.me [["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil []]
                                  ["div" nil ["me"]]]))

(deftest kw-tag-validity
  (is (= "can't have 2 #'s in a tag."
         (try (#'h/tag->tag+id+classes :div#id1#id2)
              (catch Exception e (ex-message e))))))

(deftest tag-names
  (testing "basic tags"
    (is (= "<div></div>" (str (h/html [:div])))))
  (testing "tag syntax sugar"
    (is (= "<div id=\"foo\"></div>" (str (h/html [:div#foo]))))
    (is (= "<div class=\"foo\"></div>" (str (h/html [:div.foo]))))
    (is (= "<div class=\"foo\">barbaz</div>" (str (h/html [:div.foo (str "bar" "baz")]))))
    (is (= "<div class=\"a b\"></div>" (str (h/html [:div.a.b]))))
    (is (= "<div class=\"a b c\"></div>" (str (h/html [:div.a.b.c]))))
    (is (= "<div id=\"foo\" class=\"bar baz\"></div>" (str (h/html [:div#foo.bar.baz]))))))

(deftest tag-contents
  (testing "empty tags"
    (is (= "<div></div>" (str (h/html [:div]))))
    (is (= "<h1></h1>" (str (h/html [:h1]))))
    (is (= "<script></script>" (str (h/html [:script]))))
    (is (= "<text></text>" (str (h/html [:text]))))
    (is (= "<a></a>" (str (h/html [:a]))))
    (is (= "<iframe></iframe>" (str (h/html [:iframe]))))
    (is (= "<title></title>" (str (h/html [:title]))))
    (is (= "<section></section>" (str (h/html [:section]))))
    (is (= "<select></select>" (str (h/html [:select]))))
    (is (= "<object></object>" (str (h/html [:object]))))
    (is (= "<video></video>" (str (h/html [:video])))))
  (testing "void tags"
    (is (= (str (h/html [:br])) "<br />"))
    (is (= (str (h/html [:link])) "<link />"))
    (is (= (str (h/html [:colgroup {:span 2}])) "<colgroup span=\"2\"></colgroup>"))
    (is (= (str (h/html [:colgroup [:col]])) "<colgroup><col /></colgroup>")))
  (testing "tags containing text"
    (is (= (str (h/html [:text "Lorem Ipsum"])) "<text>Lorem Ipsum</text>")))
  (testing "contents are concatenated"
    (is (= (str (h/html [:body "foo" "bar"])) "<body>foobar</body>"))
    (is (= (str (h/html [:body [:p] [:br]])) "<body><p></p><br /></body>")))
  (testing "seqs are expanded"
    (is (= (str (h/html [:body (list "foo" "bar")])) "<body>foobar</body>"))
    (is (= (str (h/html (list [:p "a"] [:p "b"]))) "<p>a</p><p>b</p>")))

  (testing "vecs don't expand - error if vec doesn't have tag name"
    ;; (is (false? (h/valid? [[:p "a"] [:p "b"]])))
    (is (= "<p>a</p><p>b</p>"
           ;; lists of hiccup are the same as fragments.
           (str (h/html (list [:p "a"] [:p "b"])))))
    (is (= "<p>a</p><p>b</p>"
           ;; lists of hiccup are the same as fragments.
           (str (h/html [:<> [:p "a"] [:p "b"]])))))
  (testing "tags can contain tags"
    (is (= (str (h/html [:div [:p]])) "<div><p></p></div>"))
    (is (= (str (h/html [:div [:b]])) "<div><b></b></div>"))
    (is (= (str (h/html [:p [:span [:a "foo"]]]))
           "<p><span><a>foo</a></span></p>"))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (is (= (str (h/html [:xml {}])) "<xml></xml>")))
  (testing "tag with populated attribute map"
    (is (= (str (h/html [:xml {:a "1", :b "2"}])) "<xml a=\"1\" b=\"2\"></xml>"))
    (is (= (str (h/html [:img {"id" "foo"}])) "<img id=\"foo\" />"))
    (is (= (str (h/html [:img {'id "foo"}])) "<img id=\"foo\" />"))
    ;; (is (= (str (h/html [:xml {:a "1", 'b "2", "c" "3"}]))
    ;;        "<xml a=\"1\" b=\"2\" c=\"3\"></xml>"))
    )
  (testing "attribute values are escaped"
    (is (= "<div id=\"&quot;\"></div>" (str (h/html [:div {:id "\""}])))))
  (testing "boolean attributes"
    (is (= "<input type=\"checkbox\" checked=\"true\" />"
           (str (h/html [:input {:type "checkbox" :checked true}]))))
    (is (= "<input type=\"checkbox\" />"
           (str (h/html [:input {:type "checkbox" :checked false}])))))
  (testing "nil attributes"
    (is (= "<span>foo</span>"
           (str (h/html [:span {:class nil} "foo"])))))
  (testing "vector attributes"
    (is (= "<span class=\"bar baz\">foo</span>"
           (str (h/html [:span {:class ["bar" "baz"]} "foo"]))))
    (is (= "<span class=\"baz\">foo</span>"
           (str (h/html [:span {:class ["baz"]} "foo"]))))
    (is (= "<span class=\"baz bar\">foo</span>"
           (str (h/html [:span {:class "baz bar"} "foo"])))))
  (testing "map attributes"
    (is (= "<span style=\"background-color:blue;color:red;opacity:100%;\">foo</span>"
           (str (h/html [:span {:style {:background-color :blue :color "red" :opacity "100%"}} "foo"])))))
  (testing "resolving conflicts between attributes in the map and tag"
    (is (= (str (h/html [:div.foo {:class "bar"} "baz"]))
           "<div class=\"bar foo\">baz</div>"))
    (is (= (str (h/html [:div.foo {:class ["bar"]} "baz"]))
           "<div class=\"bar foo\">baz</div>"))
    (is (= (str (h/html [:div#bar.foo {:id "baq"} "baz"]))
           "<div id=\"baq\" class=\"foo\">baz</div>"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (is (= (let [x "foo"] (str (h/html [:span x]))) "<span>foo</span>")))
  (testing "tag content can be forms"
    (is (= (str (h/html [:span (str (+ 1 1))])) "<span>2</span>"))
    (is (= (str (h/html [:span ({:foo "bar"} :foo)])) "<span>bar</span>")))
  (testing "attributes can contain vars"
    (let [x "foo"]
      (is (= (str (h/html [:xml {:x x}])) "<xml x=\"foo\"></xml>"))
      (is (= (str (h/html [:xml {x "x"}])) "<xml foo=\"x\"></xml>"))
      (is (= (str (h/html [:xml {:x x} "bar"])) "<xml x=\"foo\">bar</xml>"))))
  (testing "attributes are evaluated"
    (is (= (str (h/html [:img {:src (str "/foo" "/bar")}]))
           "<img src=\"/foo/bar\" />"))
    (is (= (str (h/html [:div {:id (str "a" "b")} (str "foo")]))
           "<div id=\"ab\">foo</div>")))
  (testing "type hints"
    (let [string "x"]
      (is (= (str (h/html [:span ^String string])) "<span>x</span>"))))
  (testing "optimized forms"
    (is (= (str (h/html [:ul (for [n (range 3)]
                               [:li n])]))
           "<ul><li>0</li><li>1</li><li>2</li></ul>"))
    (is (= (str (h/html [:div (if true
                                [:span "foo"]
                                [:span "bar"])]))
           "<div><span>foo</span></div>")))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (str (h/html [:div (foo)]))
      (is (= @times-called 1)))))

(deftest render-modes
  (testing "closed tag"
    (is (= "<p></p><br />" (str (h/html (list [:p] [:br])))))
    (is (= "<p></p><br />" (str (h/html [:<> [:p] [:br]])))))
  (testing "laziness and binding scope"
    (is (= "<html><link /><link /></html>" (str (h/html [:html [:link] (list [:link])])))))
  (testing "function binding scope"
    (let [f #(vector :p "<>" [:br])]
      (is (= "<p>&lt;&gt;<br /></p>" (str (h/html [f]))))
      (is (= "<p>&lt;&gt;<br /></p>" (str (h/html (f))))))))

(deftest auto-escaping
  (testing "literals"
    (is (= "&lt;&gt;"
           (str (h/html "<>"))))
    (is (= "&lt;&gt;" (str (h/html ^String (str "<>")))))
    (is (= "1"
           (str (h/html 1))))
    (is (= "2"
           (str (h/html ^Number (+ 1 1))))))
  (testing "non-literals"
    (is (= (str (h/html (list [:p "<foo>"] [:p "<bar>"])))
           "<p>&lt;foo&gt;</p><p>&lt;bar&gt;</p>"))
    (is (= (str (h/html ((constantly "<foo>")))) "&lt;foo&gt;"))
    (is (= (let [x "<foo>"] (str (h/html x))) "&lt;foo&gt;")))
  (testing "elements"
    (is (= "<p>&lt;&gt;</p>" (str (h/html [:p "<>"]))))
    (is (= "<p class=\"&lt;&quot;&gt;\"></p>"
           (str (h/html [:p {:class "<\">"}]))))
    (is (= "<p class=\"&lt;&quot;&gt;\"></p>"
           (str (h/html [:p {:class ["<\">"]}]))))
    (is (= "<ul><li>&lt;foo&gt;</li></ul>"
           (str (h/html [:ul [:li "<foo>"]])))))
  (testing "raw strings"
    (is (thrown?
          Exception
          #":hiccup/raw-html is not allowed. Maybe you meant to set allow-raw to true?"
          (str (h/html [:hiccup/raw-html "<foo>"]))))
    (is (= "<foo>" (str (h/html (h/raw "<foo>")))))
    (is (= "<p><foo></p>" (str (h/html [:p (h/raw "<foo>")]))))
    (is (= "<ul><li>&lt;&gt;</li></ul>" (str (h/html [:ul [:li "<>"]]))))))

;; in huff do not call the compiler multiple times on the same value,
;; instead use the `:hiccup/raw-html` tag

(deftest html-escaping
  (testing "precompilation"
    (is (= (str (h/html [:p "<>"])) "<p>&lt;&gt;</p>"))
    (is (= (binding [h/*escape?* false]
             (str (h/html [:p "<>"]))) "<p><></p>")))
  (testing "dynamic generation"
    (let [x [:p "<>"]]
      (is (= (str (h/html x)) "<p>&lt;&gt;</p>"))))
  (testing "attributes"
    (is (= (str (h/html [:p {:class "<>"}])) "<p class=\"&lt;&gt;\"></p>"))
    (is (= (binding [h/*escape?* false]
             (str (h/html [:p {:class "<>"}])))
           "<p class=\"<>\"></p>")))
  (testing "raw strings"
    (is (= (str (h/html [:p (h/raw "<>")])) "<p><></p>"))
    (is (= (binding [h/*escape?* false]
             (str (h/html [:p (h/raw "<>")])))
           "<p><></p>"))))
