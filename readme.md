# huff

Hiccup in pure Clojure

## Usage

`io.github.escherize/huff {:git/sha "2af31072ce08de831591ad55d8fcc247f8547eeb"}`

``` clojure
(require '[huff.core :as h])

(h/html [:div.hello#world "!"])
;; => "<div class=\"hello\" id=\"world\">!</div>"

(h/html [:<> [:div "d"] [:span "s"]])
;; => "<div>d</div><span>s</span>"

(h/html [:div {:style {:border "1px red solid"
                       :background-color "#ff00ff"}}])
;; => "<div style=\"background-color:#ff00ff;border:1px red solid;\"></div>"

(h/html [:hiccup/raw-html "<div>raw</div>"])
;; => "<div>raw</div>"

(defn str-info [s]
  [:div.info
   [:span (apply str (reverse s))]
   [:pre.len "Length: " (count s)]])

(h/html [str-info "hello"])
;; => "<div class=\"info\"><span>olleh</span><pre class=\"len\">Length: 5</pre></div>"
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
- Parse tags in any order
  - `:div#id.class` or `:div.class#id` both work
- Runs on babashka
- HTML-encoded by default
- Tested agianst slightly modified hiccup 2 tests
  - some philosophical differences
    - e.g. we dont support some shapes hiccup does, like: `{:a :b}`
- Performance is great!
  - 30% faster than hi
ccup

## Prior Art

- [hiccup](https://github.com/weavejester/hiccup)
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup)
