(define-test "(bytevector?)" (expect-static-success
  (assert-true (bytevector? #u8(1 2 3)))
  (assert-true (bytevector? #u8()))
  (assert-false (bytevector? 4))))

(define-test "(make-bytevector)" (expect-success
  (assert-equal #u8() (make-bytevector 0))
  (assert-equal #u8() (make-bytevector 0 0))
  (assert-equal #u8(0 0 0) (make-bytevector 3))
  (assert-equal #u8(5 5 5) (make-bytevector 3 5))))

(define-test "(make-bytevector) with a flonum fill fails" (expect-error type-error?
  (make-bytevector 3 5.0)))

(define-test "(make-bytevector) with a negative length fails" (expect-error range-error?
  (make-bytevector -3)))

(define-test "(bytevector)" (expect-success
  (assert-equal #u8() (bytevector))
  (assert-equal #u8(1 3 5 1 3 5) (bytevector 1 3 5 1 3 5))))

(define-test "static (bytevector) length" (expect-static-success
  (assert-equal 3 (bytevector-length #u8(1 2 3)))
  (assert-equal 0 (bytevector-length #u8()))))

(define-test "dynamic (bytevector) length" (expect-success
  (assert-equal 15 (bytevector-length (make-bytevector 15 129)))
  (assert-equal 0 (bytevector-length (make-bytevector 0 15)))))

(define-test "(bytevector-u8-ref)" (expect-success
  (define bv #u8(1 3 5 201 203 205))

  (assert-equal 1 (bytevector-u8-ref bv 0))
  (assert-equal 5 (bytevector-u8-ref bv 2))
  (assert-equal 205 (bytevector-u8-ref bv 5))))

(define-test "(bytevector-u8-ref) past end of bytevector fails" (expect-error range-error?
  (bytevector-u8-ref #u8(1 3 5 201 203 205) 7)))

(define-test "(bytevector-u8-ref) with negative index fails" (expect-error range-error?
  (bytevector-u8-ref #u8(1 3 5 201 203 205) -1)))

(define-test "(bytevector-u8-ref) with non-integer fails" (expect-error type-error?
  (bytevector-u8-ref #u8(1 3 5 201 203 205) "4")))

(define-test "(bytevector-u8-set!)" (expect #u8(1 1 2 1 1)
  ; Need to make a new bytevector because vector literals are immutable
  (define test-bytevector (make-bytevector 5 1))
  (bytevector-u8-set! test-bytevector 2 2)
  test-bytevector))

(define-test "(bytevector-u8-set!) on bytevector literal fails" (expect-error mutate-literal-error?
  ; We should fail gracefully from this - i.e. no segfault, no silent success
  (bytevector-u8-set! #u8(1 1 1 1 1 1) 2 2)))

(define-test "(bytevector-u8-set!) past end of bytevector fails" (expect-error range-error?
  (define test-bytevector (make-bytevector 5 1))
  (bytevector-u8-set! test-bytevector 5 2)))

(define-test "(bytevector-u8-set!) with negative index fails" (expect-error range-error?
  (define test-bytevector (make-bytevector 5 1))
  (bytevector-u8-set! test-bytevector -1 2)))

(define-test "(bytevector-append)" (expect-success
  (assert-equal #u8() (bytevector-append))
  (assert-equal #u8(1 2 3) (bytevector-append #u8(1 2 3)))
  (assert-equal #u8(1 2 3 4 5 6) (bytevector-append #u8(1 2) #u8(3 4) #u8(5 6)))
  (assert-equal #u8() (bytevector-append #u8() #u8() #u8()))))

(define-test "(bytevector-append) with non-bytevector fails" (expect-error type-error?
  (bytevector-append #u8(1 2) #(3 4))))

(define-test "(utf8->string)" (expect-success
  (assert-equal "" (utf8->string #u8()))
  (assert-equal "A" (utf8->string #u8(#x41)))
  (assert-equal "Hell☃!" (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21)))
  (assert-equal "☃!" (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 4))
  (assert-equal "" (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 0 0))
  (assert-equal "" (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 8 8))
  (assert-equal "ell☃" (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 1 7))))

(define-test "(utf8->string) with invalid byte sequence fails" (expect-error utf8-error?
  (utf8->string #u8(#xff))))

(define-test "(utf8->string) with backwards slice fails" (expect-error range-error?
  (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 2 1)))

(define-test "(utf8->string) past end of bytevector fails" (expect-error range-error?
  (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) 0 9)))

(define-test "(utf8->string) with negative start index fails" (expect-error range-error?
  (utf8->string #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) -1)))

(define-test "(string->utf8)" (expect-success
  (assert-equal #u8() (string->utf8 ""))
  (assert-equal #u8(#xce #xbb) (string->utf8 "λ"))
  (assert-equal #u8(#x48 #x65 #x6c #x6c #xe2 #x98 #x83 #x21) (string->utf8 "Hell☃!"))
  (assert-equal #u8(#xe2 #x98 #x83 #x21) (string->utf8 "Hell☃!" 4))
  (assert-equal #u8() (string->utf8 "Hell☃!" 0 0))
  (assert-equal #u8() (string->utf8 "Hell☃!" 5 5))
  (assert-equal #u8(#xe2 #x98 #x83) (string->utf8 "Hell☃!" 4 5))))

(define-test "(string->utf8) with backwards slice fails" (expect-error range-error?
  (string->utf8 "Hell☃!" 2 1)))

(define-test "(string->utf8) past end of string fails" (expect-error range-error?
  (string->utf8 "Hell☃!" 0 9)))

(define-test "(string->utf8) with negative start index fails" (expect-error range-error?
  (string->utf8 "Hell☃!" -1)))

(define-test "(bytevector-copy)" (expect-success
  (define a #u8(1 2 3 4 5))
  (assert-equal #u8(3 4) (bytevector-copy a 2 4))

  (define test-bytevector #u8(0 1 2 3 4 5 6 7))

  (define entire-copy (bytevector-copy test-bytevector))
  (assert-equal test-bytevector entire-copy)

  ; Mutating the copy should not affect the original bytevector
  (bytevector-u8-set! entire-copy 3 255)
  (assert-equal #u8(0 1 2 255 4 5 6 7) entire-copy)
  (assert-equal #u8(0 1 2 3 4 5 6 7) test-bytevector)

  (assert-equal #u8() (bytevector-copy test-bytevector 0 0))
  (assert-equal #u8() (bytevector-copy test-bytevector 8 8))

  (assert-equal #u8(2 3 4 5 6 7) (bytevector-copy test-bytevector 2))
  (assert-equal #u8(3 4 5) (bytevector-copy test-bytevector 3 6))))

(define-test "(bytevector-copy) with backwards slice fails" (expect-error range-error?
  (bytevector-copy #u8(0 1 2 3 4 5) 2 1)))

(define-test "(bytevector-copy) past end of bytevector fails" (expect-error range-error?
  (bytevector-copy #u8(0 1 2 3 4 5) 0 9)))

(define-test "(bytevector-copy) with negative start index fails" (expect-error range-error?
  (bytevector-copy #u8(0 1 2 3 4 5) -1)))

(define-test "(bytevector-copy!)" (expect-success
  (define a (bytevector 1 2 3 4 5))
  (define b (bytevector 10 20 30 40 50))
  (bytevector-copy! b 1 a 0 2)

  (assert-equal #u8(10 1 2 40 50) b)

  (bytevector-copy! b 1 a 0 0)
  (bytevector-copy! b 1 a 5)
  (assert-equal #u8(10 1 2 40 50) b)

  (bytevector-copy! b 0 a)
  (assert-equal #u8(1 2 3 4 5) b)))

(define-test "(bytevector-copy!) on literal fails" (expect-error mutate-literal-error?
  (define a #u8(1 2 3 4 5))
  (define b #u8(10 20 30 40 50))
  (bytevector-copy! b 1 a 0 2)))

(define-test "(bytevector-copy!) with backwards slice fails" (expect-error range-error?
  (define a (bytevector 1 2 3 4 5))
  (define b (bytevector 10 20 30 40 50))
  (bytevector-copy! b 1 a 2 0)))

(define-test "(bytevector-copy!) past end of from fails" (expect-error range-error?
  (define a (bytevector 1 2 3 4 5))
  (define b (bytevector 10 20 30 40 50))
  (bytevector-copy! b 2 a 4 6)))

(define-test "(bytevector-copy!) past end of to fails" (expect-error range-error?
  (define a (bytevector 1 2 3 4 5))
  (define b (bytevector 10 20 30 40 50))
  (bytevector-copy! b 2 a 1)))

(define-test "bytevector (hash)" (expect-success
  (import (llambda hash-map))

  (define test-bv (make-bytevector 20 0))

  ; Calculate a hash value and make sure its stable
  (define first-hash-value (hash test-bv))
  (assert-equal first-hash-value (hash test-bv))

  (bytevector-u8-set! test-bv 0 50)
  (assert-false (equal? first-hash-value (hash test-bv)))))
