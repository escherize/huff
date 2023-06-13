# huff

Hiccup in pure Clojure

## Usage

`io.github.escherize/huff {:git/sha "e1e992a276b4b542df168226ed1bc6a8e54be492"}`

```clojure

(require '[huff.core :as h])

(h/html [:div.hello#world "!"])
;; => "<div class=\"hello\" id=\"world\">!</div>"
```

## Rationale

I wanted a juicy way to write html in babashka.

- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup) is juicy, but relies on some java lib that doesn't run on babashka.
- hiccup has less features, but does run on babashka.

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
  - `:div#id.class` or `:div.class#id` both work
- Runs on babashka
- HTML-encoded by default
- Tested agianst slightly modified hiccup 2 tests
  - some philosophical differences
    - e.g. we dont support some shapes hiccup does, like: `{:a :b}`
- Performance is Good!
  - ~1.1x faster than hiccup2/hiccup

## Prior Art

- [hiccup](https://github.com/weavejester/hiccup)
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup)
