# huff

Hiccup in pure Clojure

## Usage

`io.github.escherize/huff {:git/sha "9c7b642dd093ab23f5a43311090a0bec43887e08"}`

``` clojure
(require '[huff.core :as h])
```

### Tag Parsing

Parse tags for id and class (in any order).

```clojure
(h/html [:div.hello#world "!"])
;; => <div class="hello" id="world">!</div>
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

### Style map rendering

```clojure
(h/html [:div {:style {:border "1px red solid"
                       :background-color "#ff00ff"}}])
;; => <div style="background-color:#ff00ff;border:1px red solid;"></div>
```

### Raw HTML tag:

This is nice if you want to e.g. produce markdown in the middle of your hiccup.

```clojure
(h/html [:hiccup/raw-html "<div>raw</div>"])
;; => <div>raw</div>

(h/html [:hiccup/raw-html [markdownize "# *Hello*"]])
;; => <h1><strong>Hello</strong></h1>
```

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

## Rationale

I wanted a juicy way to write html in babashka.

- [hiccup](https://github.com/weavejester/hiccup) has less features
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup) relies on some java libs that don't work on babashka.

## Features

- Reagent-like conveniences
  - style maps
  - fragments to return multiple sibling forms
  - call **functions** like **components**
   - `:hiccup/raw-html` tag to bypass compilation
  - included function to check for valid hiccup
  - attribute names mapped to idiomatic html:
    - e.g. `:background-color` -> `backgroundColor`
- Parse tags in any order
  - `:div#id.class` or `:div.class#id` both work (not the case for hiccup/hiccup)
- Runs on babashka
- HTML-encoded by default
- Tested agianst slightly modified hiccup 2 tests
  - some philosophical differences
    - e.g. we dont support some shapes hiccup does, like: `{:a :b}`
- Performance: 29% faster than hiccup/hiccup

## Prior Art

- [hiccup](https://github.com/weavejester/hiccup)
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup)
