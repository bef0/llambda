(define-library (llambda list)
  (import (scheme base))
  (import (llambda typed))
  (import (llambda nfi))

  (export cons* xcons list-tabulate iota fold reduce zip filter remove find find-tail partition take drop split-at
          take-while drop-while span break any every count append-map filter-map)


  (begin
    (define-native-library lllist (static-library "ll_llambda_list"))

    (define cons* (world-function lllist "lllist_cons_star" (-> <any> <any> * <any>)))

    (: xcons (All (D A) (-> D A (Pairof A D))))
    (define (xcons d a)
      (cons a d))

    (define list-tabulate (world-function lllist "lllist_list_tabulate" (All (A) <native-uint32> (-> <integer> A) (Listof A))))

    (define native-iota (world-function lllist "lllist_iota" (All ([N : <number>]) <native-uint32> N N (Listof N))))
    (define (iota [count : <integer>] [start : <number> 0] [step : <number> 1])
      (native-iota count start step))

    (define partition (world-function lllist "lllist_partition" (All (A) (-> <any> <boolean>) (Listof A) (Pairof (Listof A) (Listof A)))))
    (define fold (world-function lllist "lllist_fold" (All (A) (-> <any> <any> <any> * A) A (Listof <any>) (Listof <any>) * A)))

    (: reduce (All (A B) (-> (-> A A A) B (Listof A) (U A B))))
    (define (reduce proc identity lis)
      (if (null? lis)
        identity
        (begin
          (: inner-fold (All (A) (-> (-> A A A) A (Listof A) A)))
          (define (inner-fold proc accum lis)
            (if (null? lis) accum
              (inner-fold proc (proc (car lis) accum) (cdr lis))))
          (inner-fold proc (car lis) (cdr lis)))))

    (define (zip . lists) (apply map list lists))

    ; For the passed list the head's car will be bound to "value" and have "pred?" applied. If "pred?" returns true
    ; then "true-expr" will be evaluated, otherwise "false-expr". The result of the condition will be returned
    ; If the list is empty then "empty-expr" will be evaluated and returned
    (define-syntax cond-map-head
      (syntax-rules ()
                    ((cond-map-head pred? lis value true-expr false-expr)
                     (cond-map-head pred? lis value true-expr false-expr '()))
                    ((cond-map-head pred? lis value true-expr false-expr empty-expr)
                     (if (null? lis)
                       empty-expr
                       (let ((value (car lis)))
                         (if (pred? value) true-expr false-expr))))))

    (: filter (All (A) (-> (-> A <boolean>) (Listof A) (Listof A))))
    (define (filter pred? lis)
      (cond-map-head pred? lis value
                     (cons value (filter pred? (cdr lis)))
                     (filter pred? (cdr lis))))

    (: remove (All (A) (-> (-> A <boolean>) (Listof A) (Listof A))))
    (define (remove pred? lis)
      (cond-map-head pred? lis value
                     (remove pred? (cdr lis))
                     (cons value (remove pred? (cdr lis)))))

    (: find (All (A) (-> (-> A <boolean>) (Listof A) (U A #f))))
    (define (find pred? lis)
      (cond-map-head pred? lis value
                     value
                     (find pred? (cdr lis))
                     #f))

    (: find-tail (All (A) (-> (-> A <boolean>) (Listof A) (U (Pairof A (Listof A)) #f))))
    (define (find-tail pred? lis)
      (cond-map-head pred? lis value
                     lis
                     (find-tail pred? (cdr lis))
                     #f))

    (define drop (world-function lllist "lllist_drop" (-> <any> <native-uint32> <any>)))
    (define take (world-function lllist "lllist_take" (-> <any> <native-uint32> <any>)))
    (define split-at (world-function lllist "lllist_split_at" (-> <any> <native-uint32> (Pairof (Listof <any>) <any>))))

    (: take-while (All (A) (-> (-> A <boolean>) (Listof A) (Listof A))))
    (define (take-while pred? lis)
      (cond-map-head pred? lis value
                     (cons value (take-while pred? (cdr lis)))
                     '()))

    (: drop-while (All (A) (-> (-> A <boolean>) (Listof A) (Listof A))))
    (define (drop-while pred? lis)
      (cond-map-head pred? lis value
                     (drop-while pred? (cdr lis))
                     lis))

    (define span (world-function lllist "lllist_span" (All (A) (-> (-> <any> <boolean>) (Listof A) (Pairof (Listof A) (Listof A))))))
    (define break (world-function lllist "lllist_break" (All (A) (-> (-> <any> <boolean>) (Listof A) (Pairof (Listof A) (Listof A))))))

    (define any (world-function lllist "lllist_any" (All (A) (-> (-> <any> <any> * A) <list> <list> * (U #f A)))))
    (define every (world-function lllist "lllist_every" (All (A) (-> (-> <any> <any> * A) <list> <list> * (U #t A)))))
    (define count (world-function lllist "lllist_count" (-> (-> <any> <any> * <any>) <list> <list> * <native-int64>)))

    (define append-map (world-function lllist "lllist_append_map" (All (A) (-> (-> <any> <any> * (Listof A)) <list> <list> * (Listof A)))))
    (define filter-map (world-function lllist "lllist_filter_map" (All (A) (-> (-> <any> <any> * A) <list> <list> * (Listof A)))))))
