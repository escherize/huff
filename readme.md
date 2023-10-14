# huff

Hiccup in pure Clojure

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/io.github.escherize/huff.svg)](https://clojars.org/io.github.escherize/huff)

``` clojure
(require '[huff2.core :as h])
```

## Features

- *NEW*: [Extendable grammar](#extendable-grammar) + custom emitter functions!
- *NEW*: [raw-string](https://github.com/escherize/huff/issues/5) support
- Use [**functions** like **components**](#use-functons-as-components) 🪢
- Style maps work `[:. {:style {:font-size 30}}]` 🎨
- HTML-encoded by default ⛓️
- Parse tags [in any order](#tag-parsing) 🔀
  - `:div#id.c` or `:div.c#id` both work (not the case for `hiccup/hiccup`)
- Runs on [babashka](https://github.com/babashka/babashka) (unlike `lambdaisland/hiccup`) 👵
- Performance: 22-48% faster than hiccup/hiccup for runtime-generated HTML [without pre-compilation](https://github.com/escherize/huff/issues/8) 🏎
- Reagent style fragments to return multiple forms `[:<> [:li.a] [:li.b]]` 🙂
- Hiccup style fragments to return multiple forms `(list [:li.a] [:li.b])` 🙃
- `:hiccup/raw-html`: let users bypass html compilation, now with [raw-string](https://github.com/escherize/huff/issues/5)s. 📦
- Tested against slightly modified hiccup 2 tests 🩺
- Extreme shorthand syntax `[:. {:color :red}]` 💥

### Tag Parsing

Parse tags for id and class (in any order).

```clojure
(h/html [:div.hello#world "!"])
;; => <div class="hello" id="world">!</div>
```

#### Nested tag parsing

```clojure
(println (h/html [:div.left-aligned>p#user-parent>span {:id "user-name"} "Jason"]))

;=> <div class="left-aligned"><p id="user-parent"><span id="user-name">Jason</span></p></div>
```

### [reagent](https://github.com/reagent-project/reagent)-like fragment tags

```clojure
(h/html [:<> [:div "d"] [:<> [:<> [:span "s"]]]])
;; => 
<div>d</div><span>s</span>
```

This is useful for returning multiple elements from a function:

```clojure
(defn twins [x] [:<>
                 [:div.a x]
                 [:div.b x]])

(h/html [:span.parent [twins "elements"]])
;;=>
<span class="parent">
  <div class="a">elements</div>
  <div class="b">elements</div>
</span>

```

Nest and combine them with lists to better convey intent to expand:

``` clojure
(h/html
  [:ol
   [:<> (for [x [1 2]]
          [:li>p.green {:id (str "id-" x)} x])]])

;;=>
<ol>
 <li>
   <p id=\"id-1\" class=\"green\">1</p>
 </li>
 <li>
   <p id=\"id-2\" class=\"green\">2</p>
 </li>
</ol>

```

### Style map rendering

```clojure
(h/html [:div {:style {:border "1px red solid"
                       :background-color "#ff00ff"}}])
;; => <div style="background-color:#ff00ff;border:1px red solid;"></div>

(h/html [:. {:style {:width 3}}])
;;=> <div style=\"width:3px;\"></div>
```

### Raw HTML tag:

This is nice if you want to e.g. render markdown in the middle of your hiccup. Note: We disable this by default and it must be manually enabled in the call to `html` or `page`,

``` clojure

(h/html [:hiccup/raw-html "<div>raw</div>"])
;; =Throws=> :hiccup/raw-html is not allowed. Maybe you meant to set allow-raw to true?

(h/html {:allow-raw true} [:hiccup/raw-html "<div>raw</div>"])
;;=> <div>raw</div>
```

Another nice-to-have is to disallow raw html in un-trusted data getting passed into to the compiler, but being able to do that as a dev.

``` clojure
(h/html [:div [:hiccup/raw-html (h/raw-string "<!-- oops.")]])
;; => <div><!-- oops.</div>
```

[More Info]

### Use functons as components

Write a function that returns hiccup, and call it from the first position of a vector, like in [reagent](https://cljdoc.org/d/reagent/reagent/1.2.0/doc/tutorials/using-square-brackets-instead-of-parentheses-#using-greet-via--1).

```clojure
(defn str-info [s]
  [:div.info
   [:span (apply str (reverse s))]
   [:pre.len "Length: " (count s)]])

(h/html [str-info "hello"])
;; => 
<div class="info">
  <span>olleh</span>
  <pre class="len">Length: 5</pre>
</div>
```

### Automatically append px for numeric style values:

``` clojure
(h/html [:div {:style {:width (* 5 2)}}])

;;=> <div style="width:10px;"></div>
```

## Extendable Grammar

We now offer some super-deep customization of the hiccup grammar.

With this power, you can write new tags that can parse (and validate) their inputs.

### Example:

Let's say you _really_ need a tag to count its children, and put that into the final html.

1. Add your new tag to the hiccup schema:
``` clojure
(def my-schema (h2e/add-schema-branch h/hiccup-schema :my/child-counter-tag))
```

2. Write the emitter function for your tag:
``` clojure
(defmethod h/emit :my/child-counter-tag [append! [_ [_ values]] opts]
  (append! "I have " (count values) " children."))
```

`append!` takes strings and will append them internally during html generation.

3. Call huff2.core/html with your new schema:

``` clojure
;; call:
(h/html (custom-fxns! my-schema)
  [:div>h1 [:my/child-counter-tag "one" "two" "three"]])
  ;; => <div><h1>I have 3 children.</h1></div>
```

This will be a little faster, and you should prefer it if your schema isnt dynamic.

``` clojure
;; build:
(let [my-fxns (custom-fxns! my-schema)]
  (def my-html (fn my-html [hic] (h/html my-fxns))))
  
(my-html [:div>h1 [:my/child-counter-tag "one" "two" "three"]])
;; => <div><h1>I have 3 children.</h1></div>
```

More details in the [huff extension test](./test/huff/extension_test.clj).

## Prior Art

- [hiccup](https://github.com/weavejester/hiccup)
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup)
