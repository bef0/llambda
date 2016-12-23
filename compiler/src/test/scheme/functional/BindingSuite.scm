(define-test "simple let*" (expect 70
  ; This is taken from R7RS
  (let ((x 2) (y 3))
    (let* ((x 7)
       (z (+ x y)))
    (* z x)))))

(define-test "recursive procedure definition in lambda body" (expect #t
  (begin
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))

     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1)))))

     (even? 8))))

(define-test "recursive mutable procedure definition in lambda body" (expect #t
  (begin
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))

     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1)))))

     ; Setting this to a procedure returning true means (even?) will always return true
     (set! odd? (lambda (n) #t))
     (even? 7))))

(define-test "calling untyped recursive procedure with wrong arity fails at compile time" (expect-compile-error arity-error?
  ; We should peek at the signature for (takes-one) and determine this is invalid at compile time
  (begin
    (define (takes-one a) a)
    (define (takes-two a b)
      ; Error!
      (takes-one a b))

    (takes-two 1 2))))

(define-test "accessing recursive define before initialization fails" (expect-error undefined-variable-error?
  (begin
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))

     ; This used to be five-is-odd but the inliner would actually resolve this successfuly at compile time at -O 2
     (define eleven-is-odd (odd? 11))

     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1))))))))

(define-test "recursive lambda can capture non-lambda from same binding" (expect 11
  (define (count-until n)
    (define (iter p)
      (if (> p stop)
        p
        (iter (+ p 1))))
    (define stop n)
    (iter 0))

  (count-until 10)))

(define-test "hygienic scoped macro binding using let-syntax" (expect (now . outer)
  (define result1 (let-syntax ((given-that (syntax-rules ()
                     ((given-that test stmt1 stmt2 ...)
                      (if test
                      (begin stmt1
                           stmt2 ...))))))
  ; This is also a sneaky hygiene test from R7RS
  ; "if" is just a normal variable in the below code
  ; Overriding it must not interfere with the original stdlib "if" in the macro above once its expanded
  (let ((if #t))
    (given-that if (set! if 'now))
    if)))
  
  (define result2 (let ((x 'outer))
  (let-syntax ((m (syntax-rules () ((m) x))))
    (let ((x 'inner))
    (m)))))
  (cons result1 result2))) 

(define-test "hygienic scoped macro binding using letrec-syntax" (expect 7
   (letrec-syntax
     ((my-or (syntax-rules ()
                           ((my-or) #f)
                           ((my-or e) e)
                           ((my-or e1 e2 ...)
                            (let ((temp e1))
                              (if temp
                                temp
                                (my-or e2 ...)))))))
     (let ((x #f)
           (y 7)
           (temp 8)
           (let odd?)
           (if even?))

       (my-or x
              (let temp)
              (if y)
              y)))))

(define-test "simple letrec*" (expect 5
  (letrec* ((p
              (lambda (x)
                (+ 1 (q (- x 1)))))
            (q
              (lambda (y)
                (if (zero? y)
                  0
                  (+ 1 (p (- y 1))))))
            (x (p 5))
            (y x))
           y)))

(define-test "simple letrec" (expect #t
  (letrec ((even?
             (lambda (n)
               (if (zero? n)
                 #t
                 (odd? (- n 1)))))
           (odd?
             (lambda (n)
               (if (zero? n)
                 #f
                 (even? (- n 1))))))
    (even? 8))))

(define-test "recursive non-lambda definition" (expect #t
  (define self-param (cons #f (lambda () self-param)))
  (eqv? self-param ((cdr self-param)))))

(define-test "simple typed top-level define" (expect 5
  (import (llambda typed))
  (define num : <number> 5)
  num))

(define-test "simple typed declared top-level define" (expect "Hello!"
  (import (llambda typed))
  (: str <string>)
  (define str "Hello!")
  str))

(define-test "mutating typed top-level define" (expect 15.0
  (import (llambda typed))
  (define num : <number> 5)
  (set! num 15.0)
  num))

(define-test "mutating typed top-level define within initialiser fails" (expect-error undefined-variable-error?
  (define x (set! x 1))))

(define-test "typed top-level define with incompatible initialiser fails" (expect-error type-error?
  (import (llambda typed))
  (define num : <number> "not a number")))

(define-test "typed declaration with incompatible definition fails" (expect-error type-error?
  (import (llambda typed))
  (: num <number>)
  (define num "not a number")))

(define-test "mutating typed top-level define with incompatible value fails" (expect-error type-error?
  (import (llambda typed))
  (define num : <number> 5)
  (set! num "not a string")))

(define-test "simple typed inner define" (expect 5
  (import (llambda typed))
  (begin
    (define num : <number> 5)
    num)))

(define-test "mutating typed inner define" (expect 15.0
  (import (llambda typed))
  (begin
    (define num : <number> 5)
    (set! num 15.0)
    num)))

(define-test "typed inner define with incompatible initialiser fails" (expect-error type-error?
  (import (llambda typed))
  (begin
    (define num : <number> "not a number"))))

(define-test "mutating typed inner define with incompatible value fails" (expect-error type-error?
  (import (llambda typed))
  (begin
    (define num : <number> 5)
    (set! num "not a string"))))

(define-test "simple typed let" (expect 12
  (import (llambda typed))

  (let ([x : <number> 7] [y : <integer> 5])
    (+ y x))))

(define-test "typed let with incorrect type fails" (expect-error type-error?
  (import (llambda typed))

  (let ([x : <integer> 7.5])
    (+ 1 x))))

(define-test "simple typed let*" (expect 70
  (import (llambda typed))

  (let ([x : <any> 2] [y : <number> 3])
    (let* ([x : <integer> 7]
           [z : <integer> (+ x y)])
      (* z x)))))

(define-test "typed let* with incorrect type fails" (expect-error type-error?
  (import (llambda typed))

  (let ([x : <any> 2] [y : <number> 3])
    (let* ([x : <integer> 7]
           [z : <string> (+ x y)])
      (* z x)))))

(define-test "typed simple letrec*" (expect 5
  (import (llambda typed))

  (letrec* ((p : <procedure>
               (lambda (x)
                 (+ 1 (q (- x 1)))))
            (q : <procedure>
               (lambda (y)
                 (if (zero? y)
                   0
                   (+ 1 (p (- y 1))))))
            (x : <integer> (p 5))
            (y : <integer> x))
           y)))

(define-test "typed simple letrec" (expect #t
  (import (llambda typed))

  (letrec ((even? : <procedure>
                  (lambda (n)
                    (if (zero? n)
                      #t
                      (odd? (- n 1)))))
           (odd? : <procedure>
                 (lambda (n)
                   (if (zero? n)
                     #f
                     (even? (- n 1))))))
    (even? 8))))

(define-test "body (begin) splices definitions in to current scope" (expect-success
  (define a 1)
  (begin
    (define b 2))

  (assert-equal 3 (+ a b))

  (define (add-2 val)
    ; This is also a nested (begin)
    (begin
      (begin
        (define two 2)))
    (+ val two))

  (assert-equal 5 (add-2 3))))
