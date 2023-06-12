;; annotated clone of hiccup2.test
(ns huff.hiccup2-test
  (:require [clojure.test :refer [deftest are is testing]]
            [huff.core :as h]
            ;;[hiccup2.core :refer :all]
            ;;[hiccup.util :as util]
            ))

(deftest kw-tag-parsing
  (are [x y] (= (h/tag->tag+id+classes x) y)
    :div       ["div" nil []]
    :div.a     ["div" nil ["a"]]
    :div.a#d   ["div" "d" ["a"]]
    :div#d.a   ["div" "d" ["a"]]
    :div.a.b   ["div" nil ["a" "b"]]
    :div#d.a.b ["div" "d" ["a" "b"]]
    :div.a#d.b ["div" "d" ["a" "b"]]
    :div.a.b#d ["div" "d" ["a" "b"]]
    :div#d.b.a ["div" "d" ["b" "a"]]
    :div.b#d.a ["div" "d" ["b" "a"]]
    :div.a.a#d ["div" "d" ["a" "a"]]))

(deftest kw-tag-validity
  (is (= "can't have 2 #'s in a tag."
         (try (h/tag->tag+id+classes :div#id1#id2)
              (catch Exception e (ex-message e))))))

(deftest tag-names
  (testing "basic tags"
    (is (= "<div></div>" (h/html [:div]))))
  (testing "tag syntax sugar"
    (is (= "<div id=\"foo\"></div>" (h/html [:div#foo])))
    (is (= "<div class=\"foo\"></div>" (h/html [:div.foo])))
    (is (= "<div class=\"foo\">barbaz</div>" (h/html [:div.foo (str "bar" "baz")])))
    (is (= "<div class=\"a b\"></div>" (h/html [:div.a.b])))
    (is (= "<div class=\"a b c\"></div>" (h/html [:div.a.b.c])))
    (is (= "<div class=\"bar baz\" id=\"foo\"></div>" (h/html [:div#foo.bar.baz])))))

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

  (testing "keywords are not turned into strings"
    ;; in hiccup, keywords are turned into strings". not in huff.
    (is (false? (h/valid? [:div :foo]))))
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (false? (h/valid? [:div :foo])))
    (is (false? (h/valid? [[:p "a"] [:p "b"]])))
    (is (= "<p>a</p><p>b</p>"
           ;; lists of hiccup are the same as fragments.
           (h/html (list [:p "a"] [:p "b"]))))
    (is (= "<p>a</p><p>b</p>"
           ;; lists of hiccup are the same as fragments.
           (h/html [:<> [:p "a"] [:p "b"]]))))
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
    (is (= "<p></p><br />" (h/html (list [:p] [:br]))))
    (is (= "<p></p><br />" (h/html [:<> [:p] [:br]]))))
  (testing "laziness and binding scope"
    (is (= "<html><link /><link /></html>" (h/html [:html [:link] (list [:link])]))))
  (testing "function binding scope"
    (let [f #(vector :p "<>" [:br])]
      (is (= "<p>&lt;&gt;<br /></p>" (h/html [f])))
      (is (= "<p>&lt;&gt;<br /></p>" (h/html (f))))
      (is (= "<p>&lt;&gt;<br /></p>" (h/html '([f])))))))

(deftest auto-escaping
  (testing "literals"
    (is (= "&lt;&gt;"
           (h/html "<>")))
    ;; invalid in huff
    (is (false? (h/valid? :<>)))
    (is (= "&lt;&gt;"
           (h/html ^String (str "<>"))))
    (is (false? (h/valid? {"<a>" "<b>"})))
    (is (false? (h/valid? #{"<>"})))
    (is (= "1"
           (h/html 1)))
    (is (= "2"
           (h/html ^Number (+ 1 1)))))
  (testing "non-literals"
    (is (= (h/html (list [:p "<foo>"] [:p "<bar>"]))
           "<p>&lt;foo&gt;</p><p>&lt;bar&gt;</p>"))
    (is (= (h/html ((constantly "<foo>"))) "&lt;foo&gt;"))
    (is (= (let [x "<foo>"] (h/html x)) "&lt;foo&gt;")))
  (testing "elements"
    (is (= "<p>&lt;&gt;</p>" (h/html [:p "<>"])))
    (is (= "<p class=\"&lt;&quot;&gt;\"></p>"
           (h/html [:p {:class "<\">"}])))
    (is (= "<p class=\"&lt;&quot;&gt;\"></p>"
           (h/html [:p {:class ["<\">"]}])))
    (is (= "<ul><li>&lt;foo&gt;</li></ul>"
           (h/html [:ul [:li "<foo>"]]))))
  (testing "raw strings"
    (is (= "<foo>" (h/html [:hiccup/raw-html "<foo>"])))
    (is (= "<p><foo></p>" (h/html [:p [:hiccup/raw-html "<foo>"]])))
    (is (= "<ul><li>&lt;&gt;</li></ul>" (h/html [:ul [:li "<>"]])))))

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
