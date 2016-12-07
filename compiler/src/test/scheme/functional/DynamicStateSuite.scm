(define-test "parameters are procedures" (expect #t
  (define param (make-parameter 18))
  (procedure? param)))

(define-test "parameters return constant initial value if not parameterized" (expect 18
  (define param (make-parameter 18))
  (param)))

(define-test "parameters return non-constant initial value if not parameterized" (expect (1 2 3)
  (define param (make-parameter (list 1 2 3)))
  (param)))

(define-test "parameters with conversion procedure" (expect-success
  (define param (make-parameter 10 (lambda (val) (* val 2))))

  (assert-equal 20 (param))

  (parameterize ((param -5.5))
    (assert-equal -11.0 (param)))))

(define-test "initial conversion procedure can throw exception" (expect caught
  (guard (except
    ((string? except) 'caught))
    (make-parameter 15 (lambda (x)
                         (raise "Exception!"))))))

(define-test "conversion procedure with side effects is preserved is param is unused" (expect 2
  (define counter 0)
  (define unused
    (make-parameter 4 (lambda (x) (set! counter (+ counter 1)))))

  (make-parameter 4 (lambda (x) (set! counter (+ counter 1))))
  counter))


(define-test "trivial parameterize" (expect 50
  (define param (make-parameter 0))

  (parameterize ((param 50))
    (param))))

(define-test "parameterize with non-parameter fails" (expect-error invalid-argument-error?
  (define not-param +)

  (parameterize ((not-param 50))
    (not-param))))

(define-test "conversion procedure can throw exception during parameterize" (expect caught
  (define test-proc (make-parameter #f (lambda (should-throw)
    (if should-throw
      (raise "Threw exception!")))))

  (guard (except
    ((string? except) 'caught))
    (parameterize ((test-proc #t))
      'not-this))))

(define-test "nested parameterize" (expect hello
  (define param (make-parameter 0))

  (parameterize ((param 50))
    (parameterize ((param 'hello))
      (param)))))

(define-test "multiple parameter parameterize" (expect (newOne two newThree)
  (define param1 (make-parameter 'one))
  (define param2 (make-parameter 'two))
  (define param3 (make-parameter 'three))

  (parameterize ((param1 'newOne) (param3 'newThree))
    (list (param1) (param2) (param3)))))

(define-test "parameterize restores dynamic environment afterwards" (expect 0
  (define param (make-parameter 0))

  (parameterize ((param 50))
    (param))

  (param)))

(define-test "trivial dynamic-wind" (expect (56.1 . 60)
  (define testValue 0)

  (define returnValue
    (dynamic-wind
    ; Add 10 before the thunk - testValue will then be 10
    (lambda () (set! testValue (+ testValue 10)))
    ; Multiply by 4 in the thunk and return 56.1 - testValue will then be 40
    (lambda () (set! testValue (* testValue 4)) 56.1)
    ; Add 20 - testValue will then be 60
    (lambda () (set! testValue (+ testValue 20)))))

  (cons returnValue testValue)))

(define-test "dynamic-wind with incorrect before proc arity fails at compile time" (expect-compile-error type-error?
  (dynamic-wind
    (lambda (too))
    (lambda ())
    (lambda ()))))

(define-test "dynamic-wind with incorrect thunk proc arity fails at compile time" (expect-compile-error type-error?
  (dynamic-wind
    (lambda ())
    (lambda (many))
    (lambda ()))))

(define-test "dynamic-wind with incorrect after proc arity fails at compile time" (expect-compile-error type-error?
  (dynamic-wind
    (lambda ())
    (lambda ())
    (lambda (args)))))
