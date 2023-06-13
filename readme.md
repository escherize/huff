# huff

## Rationale

## Features

- parse tags in any order
- reagent-like conveniences
  - style maps
  - fragments to return multiple cibling forms
  - call **functions** like **components**
   - `:hiccup/raw-html` tag to bypass compilation
  - included function to check for valid hiccup
  - attribute names mapped to idiomatic html:
    - e.g. `:background-color` -> `backgroundColor`
- runs on babashka
- html-encoded by default
- tested agianst slightly modified hiccup 2 tests
  - some philosophical differences
    - e.g. we dont support some shapes hiccup does, like: `{:a :b}`
- performance is Ok
  - about 1.5x slower than hiccup2/hiccup
