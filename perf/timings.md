# Performance Testing
## Benching with big-page ([the wikipedia list of common misconceptions](https://en.wikipedia.org/wiki/List_of_common_misconceptions) as hiccup)
### hiccup
```
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 295.702125 ms
    Execution time std-deviation : 5.093515 ms
   Execution time lower quantile : 285.061056 ms ( 2.5%)
   Execution time upper quantile : 306.661192 ms (97.5%)
                   Overhead used : 7.091991 ns

Found 13 outliers in 60 samples (21.6667 %)
	low-severe	 3 (5.0000 %)
	low-mild	 6 (10.0000 %)
	high-mild	 1 (1.6667 %)
	high-severe	 3 (5.0000 %)
 Variance from outliers : 6.2766 % Variance is slightly inflated by outliers
```
### pre-compiled hiccup template
```
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 294.781939 ms
    Execution time std-deviation : 4.182724 ms
   Execution time lower quantile : 291.247774 ms ( 2.5%)
   Execution time upper quantile : 302.174457 ms (97.5%)
                   Overhead used : 7.091991 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 1 (1.6667 %)
	low-mild	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```
### huff
```
Evaluation count : 300 in 60 samples of 5 calls.
             Execution time mean : 227.239306 ms
    Execution time std-deviation : 10.571710 ms
   Execution time lower quantile : 217.694693 ms ( 2.5%)
   Execution time upper quantile : 253.611046 ms (97.5%)
                   Overhead used : 7.091991 ns

Found 11 outliers in 60 samples (18.3333 %)
	low-severe	 5 (8.3333 %)
	low-mild	 6 (10.0000 %)
 Variance from outliers : 31.9964 % Variance is moderately inflated by outliers
```


## Benching with medium_page ([a github Issue page](https://github.com/escherize/huff/issues/8) ~ 40kbp)
### hiccup
```
Evaluation count : 1320 in 60 samples of 22 calls.
             Execution time mean : 46.369788 ms
    Execution time std-deviation : 764.313065 µs
   Execution time lower quantile : 45.722580 ms ( 2.5%)
   Execution time upper quantile : 47.554377 ms (97.5%)
                   Overhead used : 7.091991 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 1 (1.6667 %)
	low-mild	 1 (1.6667 %)
 Variance from outliers : 5.6508 % Variance is slightly inflated by outliers
```
### pre-compiled hiccup template
```
Evaluation count : 1320 in 60 samples of 22 calls.
             Execution time mean : 47.383825 ms
    Execution time std-deviation : 1.103090 ms
   Execution time lower quantile : 46.392809 ms ( 2.5%)
   Execution time upper quantile : 49.398033 ms (97.5%)
                   Overhead used : 7.091991 ns
```
### huff
```
Evaluation count : 2340 in 60 samples of 39 calls.
             Execution time mean : 25.463353 ms
    Execution time std-deviation : 554.140695 µs
   Execution time lower quantile : 24.811835 ms ( 2.5%)
   Execution time upper quantile : 27.012070 ms (97.5%)
                   Overhead used : 7.091991 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 9.4501 % Variance is slightly inflated by outliers
```
