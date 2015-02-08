(import (scheme base))
(import (scheme write))
(import (scheme file))

(import (llambda typed))
(import (llambda list))

(define-record-type <error-category> (error-category scheme-name nfi-name enum-name r7rs) error-category?
  ([scheme-name : <string>] error-category-scheme-name)
  ([nfi-name : <string>] error-category-nfi-name)
  ([enum-name : <string>] error-category-enum-name)
  ([r7rs : <boolean>] error-category-r7rs?))

(define default-error-category (error-category "default-error" "default_error" "Default" #t))

(define unindexed-categories (list
  default-error-category
  (error-category "file-error" "file_error" "File" #t)
  (error-category "read-error" "read_error" "Read" #t)
  (error-category "type-error" "type_error" "Type" #f)
  (error-category "arity-error" "arity_error" "Arity" #f)
  (error-category "range-error" "range_error" "Range" #f)
  (error-category "utf8-error" "utf8_error" "Utf8" #f)
  (error-category "divide-by-zero-error" "divide_by_zero_error" "DivideByZero" #f)
  (error-category "mutate-literal-error" "mutate_literal_error" "MutateLiteral" #f)
  (error-category "undefined-variable-error" "undefined_variable_error" "UndefinedVariable" #f)
  (error-category "out-of-memory-error" "out_of_memory_error" "OutOfMemory" #f)
  (error-category "invalid-argument-error" "invalid_argument_error" "InvalidArgument" #f)
  (error-category "integer-overflow-error" "integer_overflow_error" "IntegerOverflow" #f)
  (error-category "implementation-restriction-error" "implementation_restriction_error" "ImplementationRestriction" #f)
  (error-category "unclonable-value-error" "unclonable_value_error" "UnclonableValue" #f)
  (error-category "no-actor-error" "no_actor_error" "NoActor" #f)
  (error-category "expired-escape-procedure-error" "expired_escape_procedure" "ExpiredEscapeProcedure" #f)))

(define (error-category-pred-name [cat : <error-category>])
  (string-append (error-category-scheme-name cat) "?"))

(define (error-category-pred-function [cat : <error-category>])
  (string-append "llerror_is_" (error-category-nfi-name cat)))

(define (error-category-raise-name [cat : <error-category>])
  (string-append "raise-" (error-category-scheme-name cat)))

(define (error-category-raise-function [cat : <error-category>])
  (string-append "llerror_raise_" (error-category-nfi-name cat)))

(: number-categories (-> <exact-integer> (Listof <error-category>) (Listof (Pairof <exact-integer> <error-category>))))
(define (number-categories start-at categories)
  (if (null? categories)
    categories
    (cons (cons start-at (car categories)) (number-categories (+ 1 start-at) (cdr categories)))))

; Assign enum numbers to each category
(define indexed-categories (number-categories 0 unindexed-categories))

; Writes out a generic generated file message
(define (write-generated-file-warning)
  (display "This file is automatically generated by generate-error-categories.scm. Do not edit manually."))

; Writes out ErrorCategory.h
(define (write-error-category-h)
  (display "#ifndef _LLIBY_BINDING_GENERATED_ERRORCATEGORY_H\n")
  (display "#define _LLIBY_BINDING_GENERATED_ERRORCATEGORY_H\n")
  (newline)

  (display "// ")
  (write-generated-file-warning)
  (display "\n")

  (newline)
  (display "#include <cstdint>\n")
  (newline)

  (display "namespace lliby\n")
  (display "{\n")
  (newline)

  (display "enum class ErrorCategory : std::uint16_t\n")
  (display "{\n")

  (for-each (lambda (indexed-category)
    (define index (car indexed-category))
    (define category (cdr indexed-category))

    (display "\t")
    (display (error-category-enum-name category))
    (display " = ")
    (display index)
    (display ",\n")
  ) indexed-categories)

  (display "};\n")
  (newline)

  (display "const char *schemeNameForErrorCategory(ErrorCategory category);\n")
  (newline)

  (display "}\n")
  (newline)

  (display "#endif\n"))

(define (write-error-category-cpp)
  (display "#include \"ErrorCategory.h\"\n")
  (newline)

  (display "namespace lliby\n")
  (display "{\n")
  (newline)

  (display "const char *schemeNameForErrorCategory(ErrorCategory category)\n")
  (display "{\n")
  (display "\tswitch(category)\n")
  (display "\t{\n")

  (for-each (lambda (cat)
    (display "\tcase ErrorCategory::")
    (display (error-category-enum-name cat))
    (display ":\n")

    (display "\t\treturn ")
    (write (error-category-scheme-name cat))
    (display ";\n")
  ) unindexed-categories)

  (display "\t}\n")
  (display "}\n")

  (newline)
  (display "}\n"))

; Writes out ErrorCategory.scala
(define (write-error-category-scala)
  (display "package io.llambda.compiler\n")
  (display "import io.llambda\n")
  (newline)

  (display "// ")
  (write-generated-file-warning)
  (display "\n")

  (newline)
  (display "sealed abstract class ErrorCategory(val runtimeId : Int)\n")
  (newline)
  (display "object ErrorCategory {\n")

  (for-each (lambda (indexed-category)
    (define index (car indexed-category))
    (define category (cdr indexed-category))

    (display "  object ")
    (display (error-category-enum-name category))
    (display " extends ErrorCategory(")
    (display index)
    (display ")\n")
  ) indexed-categories)

  (newline)
  (display "  def fromPredicate : PartialFunction[String, ErrorCategory] = {\n")

  (for-each (lambda (indexed-category)
    (define index (car indexed-category))
    (define category (cdr indexed-category))

    (display "    case ")
    (write (error-category-pred-name category))
    (display " => ")
    (display (error-category-enum-name category))
    (newline)
  ) indexed-categories)

  (display "  }\n")

  (display "}\n"))

; Writes out error.scm
(define (write-error-scheme-library)
  (define pred-symbols (map (lambda (cat)
                              (string->symbol (error-category-pred-name cat)))
                            (remove error-category-r7rs? unindexed-categories)))

  (define raise-symbols (map (lambda (cat)
                               (string->symbol (error-category-raise-name cat)))
                             (remove error-category-r7rs? unindexed-categories)))

  (display "; ")
  (write-generated-file-warning)
  (display "\n")

  (display "(define-library (llambda error)\n")
  (display "  (import (llambda internal primitives))\n")
  (display "  (import (llambda typed))\n")
  (display "  (import (llambda nfi))\n")
  (newline)

  (display "  ")
  (write (cons 'export (append pred-symbols raise-symbols)))
  (newline)

  (display "  (begin\n")
  (display "    (define-native-library llerror (static-library \"ll_llambda_error\"))")
  (newline)

  (for-each (lambda (cat)
    (unless (error-category-r7rs? cat)
      (define pred-symbol (string->symbol (error-category-pred-name cat)))
      (define pred-func-name (error-category-pred-function cat))

      (display "    ")
      (write `(define ,pred-symbol ,`(native-function llerror ,pred-func-name (-> <any> <native-bool>))))
      (display "\n"))

    (unless (equal? default-error-category cat)
      (define raise-symbol (string->symbol (error-category-raise-name cat)))
      (define raise-func-name (error-category-raise-function cat))

      (display "    ")
      (write `(define ,raise-symbol ,`(world-function llerror ,raise-func-name (-> <string> <any> * <unit>) noreturn)))
      (display "\n"))
  ) unindexed-categories)

  (display "))\n"))

; Writes out error.cpp
(define (write-error-cpp)
  (display "// ")
  (write-generated-file-warning)
  (display "\n")

  (newline)
  (display "#include \"binding/AnyCell.h\"\n")
  (display "#include \"binding/ErrorObjectCell.h\"\n")
  (display "#include \"binding/ErrorCategory.h\"\n")
  (display "#include \"dynamic/SchemeException.h\"\n")
  (newline)
  (display "using namespace lliby;\n")
  (newline)
  (display "namespace\n")
  (display "{\n")
  (display "\tbool isErrorObjectOfCategory(AnyCell *obj, ErrorCategory expected)\n")
  (display "\t{\n")
  (display "\t\tif (auto errorObj = cell_cast<ErrorObjectCell>(obj))\n")
  (display "\t\t{\n")
  (display "\t\t\treturn errorObj->category() == expected;\n")
  (display "\t\t}\n")
  (display "\n")
  (display "\t\treturn false;\n")
  (display "\t}\n")
  (display "}\n")

  (newline)
  (display "extern \"C\"\n")
  (display "{\n")
  (newline)

  (for-each (lambda (cat)
    (define enum-name (error-category-enum-name cat))

    (unless (error-category-r7rs? cat)
      (define pred-func-name (error-category-pred-function cat))

      (newline)
      (display "bool ")
      (display pred-func-name)
      (display "(AnyCell *obj)\n")

      (display "{\n")

      (display "\treturn isErrorObjectOfCategory(obj, ErrorCategory::")
      (display enum-name)
      (display ");\n")

      (display "}\n"))

    (unless (equal? default-error-category cat)
      (define raise-func-name (error-category-raise-function cat))

      (newline)
      (display "void ")
      (display raise-func-name)
      (display "(World &world, StringCell *message, RestValues<AnyCell> *irritants)\n")

      (display "{\n")

      (display "\tthrow dynamic::SchemeException(")
      (display "ErrorObjectCell::createInstance(world, message, irritants, ErrorCategory::")
      (display enum-name)
      (display "));\n")

      (display "}\n"))


  ) unindexed-categories)

  (newline)
  (display "}\n"))

(with-output-to-file "runtime/binding/generated/ErrorCategory.h" write-error-category-h)
(with-output-to-file "runtime/binding/generated/ErrorCategory.cpp" write-error-category-cpp)

(with-output-to-file "compiler/src/main/scala/generated/ErrorCategory.scala" write-error-category-scala)
(with-output-to-file "compiler/src/main/scheme/libraries/llambda/error.scm" write-error-scheme-library)
(with-output-to-file "runtime/stdlib/llambda/error/error.cpp" write-error-cpp)
