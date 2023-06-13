# huff

## Rationale

I wanted a juicy way to write html in babashka.

- lambdaisland/hiccup is juicy, but doesn't run on babashka
- hiccup is not that juicy, but runs on babashka.

## Features

- Reagent-like conveniences
  - style maps
  - fragments to return multiple cibling forms
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
- Performance is Great!
  - ~1.1x faster than hiccup2/hiccup

## Prior Art

- [hiccup](https://github.com/weavejester/hiccup)
- [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup)
