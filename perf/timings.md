# Performance Testing

Benched using criterium while loading the html->hiccup'd version of [wikipedia's list of common misconceptions](https://en.wikipedia.org/wiki/List_of_common_misconceptions), about 400kb of html:

## hiccup/hiccup
```
            Evaluation count : 300 in 60 samples of 5 calls.
         Execution time mean : 303.045596 ms
Execution time std-deviation : 7.867078 ms
```

## io.github.escehrize/huff

```
Evaluation count : 300 in 60 samples of 5 calls.
             Execution time mean : 214.016159 ms
    Execution time std-deviation : 7.610575 ms
```

(303-214)/303 = 29%



----------

# Old Notes:

## defmethod -> case

### basis

```
Evaluation count : 144 in 6 samples of 24 calls.
             Execution time mean : 4.540215 ms
    Execution time std-deviation : 799.269444 µs
   Execution time lower quantile : 4.078281 ms ( 2.5%)
   Execution time upper quantile : 5.927349 ms (97.5%)
                   Overhead used : 6.946770 ns

Found 1 outliers in 6 samples (16.6667 %)
	low-severe	 1 (16.6667 %)
 Variance from outliers : 47.8992 % Variance is moderately inflated by outliers
```

### case instead of defmethod
```
Evaluation count : 168 in 6 samples of 28 calls.
             Execution time mean : 4.838997 ms
    Execution time std-deviation : 711.183080 µs
   Execution time lower quantile : 4.210252 ms ( 2.5%)
   Execution time upper quantile : 5.830714 ms (97.5%)
                   Overhead used : 6.946770 ns
```

### result

slower with case


## iolist approach + re-string

- note: did not extend it to classes, or styles....

```
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 459.982626 ms
    Execution time std-deviation : 16.258712 ms
   Execution time lower quantile : 444.237452 ms ( 2.5%)
   Execution time upper quantile : 486.031458 ms (97.5%)
                   Overhead used : 6.892290 ns

Found 1 outliers in 6 samples (16.6667 %)
    low-severe   1 (16.6667 %)
 Variance from outliers : 13.8889 % Variance is moderately inflated by outliers
 ```

### result: faster.

## iolist approach + deeper iolist optimization:

### Benching hiccup

```
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 303.045596 ms
    Execution time std-deviation : 7.867078 ms
   Execution time lower quantile : 290.817816 ms ( 2.5%)
   Execution time upper quantile : 318.398702 ms (97.5%)
                   Overhead used : 6.892290 ns

Found 1 outliers in 60 samples (1.6667 %)
 low-severe 1 (1.6667 %)
 Variance from outliers : 12.6464 % Variance is moderately inflated by outliers```

### Benching huff

```
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 297.700489 ms
    Execution time std-deviation : 12.244046 ms
   Execution time lower quantile : 284.300660 ms ( 2.5%)
   Execution time upper quantile : 330.359573 ms (97.5%)
                   Overhead used : 6.892290 ns

Found 8 outliers in 60 samples (13.3333 %)
 low-severe 4 (6.6667 %)
 low-mild   4 (6.6667 %)
 Variance from outliers : 27.0975 % Variance is moderately inflated by outliers```

### Result: fast!!!!!!
