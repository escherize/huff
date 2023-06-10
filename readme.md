# huff

## Rationale

There's already [hiccup](https://github.com/weavejester/hiccup) and [lambdaisland/hiccup](https://github.com/lambdaisland/hiccup).

|   | huff          | hiccup | hiccup2 | lambdaisland/hiccup |
| - | ------------- | ------ | ---- | ---- |
| Runnable in babashka | ✅ | ✅ |❌ |✅ |
| Auto-escape strings | ✅ | sorta | ✅ |
| Fragments `([:<> ...])` | ✅ | ❌ | ✅ |❌ |
| Components `([my-fn ...])` | ✅ | ❌ | ✅ | ❌ |
| Style maps `([:div {:style {:color "blue"}}])` | ✅ | ✅ | ✅ |✅ | 
| Insert pre-rendered HTML with `[::hiccup/unsafe-html "your html"]` | ✅ | ❌ | ✅ |❌ | 
| Can export sgml and xml | ❌ | ✅ | ✅ | ? |

I wanted to start writing more html from babashka, so here is [huff](https://github.com/escherize/huff).
