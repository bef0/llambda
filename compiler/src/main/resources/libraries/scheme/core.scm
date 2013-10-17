(define-library (scheme core)
	(import (llambda primitives))
	(import (llambda nfi))
	(export lambda quote if set! syntax-error)

	(export begin)
	(begin
	  (define-syntax begin
		 (syntax-rules ()
							((begin exp ...)
							 ((lambda () exp ...))))))

	(export let)
	(begin
	  ; This isn't the full definition - tagged let isn't supported
	  (define-syntax let
		 (syntax-rules ()
							((let ((name val) ...) body1 body2 ...)
							 ((lambda (name ...) body1 body2 ...)
							  val ...)))))

	(export and or when)
	(begin
	  (define-syntax and
		 (syntax-rules ()
							((and) #t)
							((and test) test)
							((and test1 test2 ...)
							 (if test1 (and test2 ...) #f))))

	  (define-syntax or
		 (syntax-rules ()
							((or) #f)
							((or test) test)
							((or test1 test2 ...)
							 (let ((x test1))
								(if x x (or test2 ...))))))

	  (define-syntax when
		 (syntax-rules ()
							((when test result1 result2 ...)
							 (if test
								(begin result1 result2 ...))))))

	(export number? real? rational? exact? exact-integer? inexact? cos sin tan exact inexact)
	(begin
	  (define number? (native-function "lliby_is_numeric" (boxed-datum) bool))
	  ; We only support real and rational numbers
	  (define real? number?)
	  (define rational? number?)
	  
	  (define exact? (native-function "lliby_is_exact_integer" (boxed-datum) bool))
	  (define exact-integer? exact?)
	  (define inexact? (native-function "lliby_is_inexact_rational" (boxed-datum) bool))
	  
	  ; These always return inexact numbers so we can use the C standard
	  ; library directly
	  (define sin (native-function "sin" (double) double))
	  (define cos (native-function "cos" (double) double))
	  (define tan (native-function "tan" (double) double))
	  
	  (define exact (native-function "lliby_exact" (boxed-numeric) int64))
	  (define inexact (native-function "lliby_inexact" (boxed-numeric) double)))

	(export boolean? not)
	(begin
	  (define boolean? (native-function "lliby_is_boolean" (boxed-datum) bool))
	  (define not (native-function "lliby_not" (bool) bool)))

	(export write)
	(begin
	  (define write (native-function "lliby_write" (boxed-datum) void)))

	(export pair? null? cons car cdr set-car! set-cdr! length)
	(begin 
	  (define pair? (native-function "lliby_is_pair" (boxed-datum) bool))
	  (define null? (native-function "lliby_is_empty_list" (boxed-datum) bool))
	  (define cons (native-function "lliby_cons" (boxed-datum boxed-datum) boxed-pair))
	  (define car (native-function "lliby_car" (boxed-pair) boxed-datum))
	  (define cdr (native-function "lliby_cdr" (boxed-pair) boxed-datum))
	  (define set-car! (native-function "lliby_set_car" (boxed-pair boxed-datum) void))
	  (define set-cdr! (native-function "lliby_set_cdr" (boxed-pair boxed-datum) void))
	  (define length (native-function "lliby_length" (boxed-list-element) uint32)))

	(export char? digit-value char->integer integer->char)
	(begin
	  (define char? (native-function "lliby_is_character" (boxed-datum) bool))
	  (define digit-value (native-function "lliby_digit_value" (unicode-char) boxed-datum))
	  (define char->integer (native-function "lliby_char_to_integer" (unicode-char) int32))
	  (define integer->char (native-function "lliby_integer_to_char" (int32) unicode-char)))
	
	(export vector? make-vector vector-ref vector-set!)
	(begin
	  (define vector? (native-function "lliby_is_vector" (boxed-datum) bool))
	  (define make-vector (native-function "lliby_make_vector" (uint32 boxed-datum) boxed-vector))
	  (define vector-ref (native-function "lliby_vector_ref" (boxed-vector uint32) boxed-vector))
	  (define vector-set! (native-function "lliby_vector_set" (boxed-vector uint32 boxed-datum) void)))
)
