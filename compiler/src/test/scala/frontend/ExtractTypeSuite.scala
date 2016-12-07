package io.llambda.compiler.frontend
import io.llambda

import org.scalatest.FunSuite

import llambda.compiler._
import llambda.compiler.{valuetype => vt}
import llambda.compiler.valuetype.Implicits._

class ExtractTypeSuite extends FunSuite with testutil.ExprHelpers {
  val primitiveScope = new ImmutableScope(collection.mutable.Map(Primitives.bindings.toSeq : _*))
  val nfiScope = new ImmutableScope(testutil.NfiExports(), Some(primitiveScope))
  
  test("define simple type aliases") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))
    bodyFor("(define-type <custom-type> <native-int32>)")(scope)

    assert(scope("<custom-type>") === BoundType(vt.Int32))

    intercept[UnboundVariableException] {
      bodyFor("(define-type <another-type> <doesnt-exist>)")(scope)
    }
    
    intercept[BadSpecialFormException] {
      // Not enough args
      bodyFor("(define-type <another-type>)")(scope)
    }
    
    intercept[BadSpecialFormException] {
      // Too many arguments
      bodyFor("(define-type <another-type> <native-int32> <unicode-char>)")(scope)
    }
  }

  test("redefining type fails") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    intercept[DuplicateDefinitionException] {
      bodyFor("""
        (define-type <custom-type> <native-int32>)
        (define-type <custom-type> <native-int64>)
      """)(scope)
    }
  }

  test("defining union types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))
    bodyFor("(define-type <custom-type> (U <string> <port>))")(scope)

    assert(scope("<custom-type>") ===
      BoundType(vt.UnionType(Set(vt.PortType, vt.StringType)))
    )

    // Single type unions should degrade to that exact typer
    bodyFor("(define-type <single-type-union> (U <string>))")(scope)
    assert(scope("<single-type-union>") === BoundType(vt.StringType))
    
    // Unions of unions should break down to their member types
    // Also we should ignore duplicate types (<empty-list> in this case)
    bodyFor("(define-type <union-of-union> (U <list-element> <empty-list> <number>))")(scope)
    assert(scope("<union-of-union>") ===
      BoundType(vt.UnionType(Set(
        vt.ExactIntegerType,
        vt.FlonumType,
        vt.EmptyListType,
        vt.AnyPairType
      )))
    )

    intercept[UnboundVariableException] {
      // Type doesn't exist
      bodyFor("(define-type <another-type> (U <doesnt-exist>))")(scope)
    }
    
    intercept[MalformedExprException] {
      // Using a non-type constructor
      bodyFor("(define-type <another-type> (if <string>))")(scope)
    }
    
    intercept[BadSpecialFormException] {
      // Using a native type
      bodyFor("(define-type <another-type> (U <native-int32>))")(scope)
    }
  }

  test("defining literal boolean types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <custom-false> #f)")(scope)
    assert(scope("<custom-false>") === BoundType(vt.LiteralBooleanType(false)))

    bodyFor("(define-type <custom-quoted-false> '#f)")(scope)
    assert(scope("<custom-quoted-false>") === BoundType(vt.LiteralBooleanType(false)))

    bodyFor("(define-type <custom-true> #t)")(scope)
    assert(scope("<custom-true>") === BoundType(vt.LiteralBooleanType(true)))

    bodyFor("(define-type <custom-boolean> (U #f #t))")(scope)
    assert(scope("<custom-boolean>") === BoundType(vt.BooleanType))
  }

  test("empty list shorthand") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <custom-null> '())")(scope)
    assert(scope("<custom-null>") === BoundType(vt.EmptyListType))
  }

  test("defining literal symbol types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <unescaped> 'hello)")(scope)
    assert(scope("<unescaped>") === BoundType(vt.LiteralSymbolType("hello")))

    bodyFor("(define-type <escaped> '|Hello, world!|)")(scope)
    assert(scope("<escaped>") === BoundType(vt.LiteralSymbolType("Hello, world!")))
  }

  test("defining pair types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <string-pair> (Pairof <string> <string>))")(scope)
    assert(scope("<string-pair>") === BoundType(vt.PairType(vt.StringType, vt.StringType)))

    bodyFor("(define-type <nested-pair> (Pairof <symbol> (Pairof <string> <port>)))")(scope)
    assert(scope("<nested-pair>") === BoundType(
      vt.PairType(
        vt.SymbolType,
        vt.PairType(
          vt.StringType,
          vt.PortType
        )
      )
    ))
    
    intercept[BadSpecialFormException] {
      // Too many arguments
      bodyFor("(define-type <too-many-args> (Pairof <string> <string> <string>))")(scope)
    }
    
    intercept[BadSpecialFormException] {
      // Not enough arguments
      bodyFor("(define-type <insufficient-args> (Pairof <string>))")(scope)
    }
  }

  test("defining homogeneous list types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))
    val stringListType = vt.UniformProperListType(vt.StringType)

    bodyFor("(define-type <string-list> (Listof <string>))")(scope)
    assert(scope("<string-list>") === BoundType(stringListType))
    
    bodyFor("(define-type <string-list-list> (Listof (Listof <string>)))")(scope)
    assert(scope("<string-list-list>") === BoundType(vt.UniformProperListType(stringListType)))

    intercept[BadSpecialFormException] {
      // Too many arguments
      bodyFor("(define-type <too-many-args> (Listof <string> <string>))")(scope)
    }
    
    intercept[BadSpecialFormException] {
      // Not enough arguments
      bodyFor("(define-type <insufficient-args> (Listof))")(scope)
    }
  }

  test("defining specified type lists") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <other-empty-list> (List))")(scope)
    assert(scope("<other-empty-list>") === BoundType(vt.EmptyListType))
    
    bodyFor("(define-type <string-tuple> (List <string>))")(scope)
    assert(scope("<string-tuple>") === BoundType(
      vt.SpecificPairType(
        vt.StringType,
        vt.EmptyListType
      )
    ))
    
    bodyFor("(define-type <string-symbol-tuple> (List <string> <symbol>))")(scope)
    assert(scope("<string-symbol-tuple>") === BoundType(
      vt.SpecificPairType(
        vt.StringType,
        vt.SpecificPairType(
          vt.SymbolType,
          vt.EmptyListType
        )
      )
    ))
    
    bodyFor("(define-type <recursive-tuple> (Rec T (List <string> <symbol> T)))")(scope)
    assert(scope("<recursive-tuple>") === BoundType(
      vt.SpecificPairType(
        vt.StringType,
        vt.SpecificPairType(
          vt.SymbolType,
          vt.SpecificPairType(
            vt.RecursiveSchemeTypeRef(2),
            vt.EmptyListType
          )
        )
      )
    ))
  }

  test("defining procedure types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <string-proc> (-> <string>))")(scope)
    assert(scope("<string-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=Nil,
        optionalArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.StringType)
      )
    ))

    bodyFor("(define-type <values-to-string-proc> (-> <port> <symbol> <string>))")(scope)
    assert(scope("<values-to-string-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=List(vt.PortType, vt.SymbolType),
        optionalArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.StringType)
      )
    ))

    bodyFor("(define-type <symbol-to-values-proc> (-> <symbol> (Values <exact-integer> <flonum>)))")(scope)
    assert(scope("<symbol-to-values-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=List(vt.SymbolType),
        optionalArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.MultipleValues(
          vt.SpecificProperListType(List(vt.ExactIntegerType, vt.FlonumType))
        )
      )
    ))

    bodyFor("(define-type <values-with-rest-to-arbitrary-proc> (-> <port> <symbol> <pair> * *))")(scope)
    assert(scope("<values-with-rest-to-arbitrary-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=List(vt.PortType, vt.SymbolType),
        optionalArgTypes=Nil,
        restArgMemberTypeOpt=Some(vt.AnyPairType),
        returnType=vt.ReturnType.ArbitraryValues
      )
    ))

    intercept[BadSpecialFormException] {
      bodyFor("(define-type <insufficient-args> (->))")(scope)
    }

    bodyFor("(define-type <optional-string-proc> (->* () (<string>) <string>))")(scope)
    assert(scope("<optional-string-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=Nil,
        optionalArgTypes=List(vt.StringType),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.StringType)
      )
    ))

    bodyFor("(define-type <optional-with-rest-proc> (->* (<symbol>) (<string> <port>) <number> * <string>))")(scope)
    assert(scope("<optional-with-rest-proc>") === BoundType(
      vt.ProcedureType(
        mandatoryArgTypes=List(vt.SymbolType),
        optionalArgTypes=List(vt.StringType, vt.PortType),
        restArgMemberTypeOpt=Some(vt.NumberType),
        returnType=vt.ReturnType.SingleValue(vt.StringType)
      )
    ))
  }

  test("defining case procedure types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))
    
    bodyFor("(define-type <zero-case-proc> (case->))")(scope)
    assert(scope("<zero-case-proc>") === BoundType(vt.CaseProcedureType(Nil)))
    
    bodyFor("(define-type <one-case-proc> (case-> (-> <string>)))")(scope)
    assert(scope("<one-case-proc>") === BoundType(
      vt.CaseProcedureType(List(
        vt.ProcedureType(
          mandatoryArgTypes=Nil,
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.SingleValue(vt.StringType)
        )
      ))
    ))

    bodyFor("(define-type <two-case-proc> (case-> (-> <number>) (-> <string> <number>)))")(scope)
    assert(scope("<two-case-proc>") === BoundType(
      vt.CaseProcedureType(List(
        vt.ProcedureType(
          mandatoryArgTypes=Nil,
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.SingleValue(vt.NumberType)
        ),
        vt.ProcedureType(
          mandatoryArgTypes=List(vt.StringType),
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.SingleValue(vt.NumberType)
        )
      ))
    ))
    
    bodyFor("(define-type <two-case-with-rest-proc> (case-> (-> <number>) (-> <string> * <number>)))")(scope)
    assert(scope("<two-case-with-rest-proc>") === BoundType(
      vt.CaseProcedureType(List(
        vt.ProcedureType(
          mandatoryArgTypes=Nil,
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.SingleValue(vt.NumberType)
        ),
        vt.ProcedureType(
          mandatoryArgTypes=Nil,
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=Some(vt.StringType),
          returnType=vt.ReturnType.SingleValue(vt.NumberType)
        )
      ))
    ))
 
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <non-proc-case-fails> (case-> (-> <string>) <string>))")(scope)
    }
      
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <same-arity-fails> (case-> (-> <string>) (-> <string>)))")(scope)
    }
      
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <decreasing-arity-fails> (case-> (-> <number> <string>) (-> <string>)))")(scope)
    }
      
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <after-rest-fails> (case-> (-> <symbol> * <string>) (-> <number> <string>)))")(scope)
    }
  }

  test("defining hash map types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    bodyFor("(define-type <string-to-symbol-hash-map> (HashMap <string> <symbol>))")(scope)
    assert(scope("<string-to-symbol-hash-map>") === BoundType(vt.HashMapType(vt.StringType, vt.SymbolType)))

    bodyFor("(define-type <empty-hash-map> (HashMap (U) (U)))")(scope)
    assert(scope("<empty-hash-map>") === BoundType(vt.HashMapType(vt.EmptySchemeType, vt.EmptySchemeType)))
  }

  test("defining recursive types") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))
    val stringListType = vt.UniformProperListType(vt.StringType)
    
    bodyFor("(define-type <manual-string-list> (Rec PL (U <empty-list> (Pairof <string> PL))))")(scope)
    assert(scope("<manual-string-list>") === BoundType(stringListType))
    
    bodyFor("(define-type <implicit-recursive> (U <empty-list> (Pairof <string> <implicit-recursive>)))")(scope)
    assert(scope("<implicit-recursive>") === BoundType(stringListType))
    
    bodyFor("(define-type <string-tree> (Rec BT (U <string> (Pairof BT BT))))")(scope)
    assert(scope("<string-tree>") === BoundType(
      vt.UnionType(Set(
        vt.StringType,
        vt.SpecificPairType(
          vt.RecursiveSchemeTypeRef(1),
          vt.RecursiveSchemeTypeRef(1)
        )
      ))
    ))

    // This isn't a meaninful type; don't think about it too hard
    // This is just checking (Listof) can take a type reference
    bodyFor("(define-type <list-list> (Rec LL (Listof LL)))")(scope)
    assert(scope("<list-list>") === BoundType(
      vt.UnionType(Set(
        vt.EmptyListType,
        vt.SpecificPairType(
          vt.RecursiveSchemeTypeRef(1),
          vt.RecursiveSchemeTypeRef(1)
        )
      ))
    ))

    bodyFor("(define-type <list-of-pairs-to-list> (Rec W (Listof (Pairof <boolean> W))))")(scope)
    assert(scope("<list-of-pairs-to-list>") === BoundType(
      vt.UnionType(Set(
        vt.EmptyListType,
        vt.SpecificPairType(
          vt.SpecificPairType(
            vt.BooleanType,
            vt.RecursiveSchemeTypeRef(2)
          ),
          vt.RecursiveSchemeTypeRef(1)
        )
      ))
    ))
    
    intercept[BadSpecialFormException] {
      // Unions can't have recursive types
      bodyFor("(define-type <inside-union> (Rec UT (U <string> UT)))")(scope)
    }
      
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <too-many-args> (Rec BT BT (U <string> (Pairof BT BT))))")(scope)
    }
      
    intercept[BadSpecialFormException] {
      bodyFor("(define-type <insufficient-args> (Rec (U <string> (Pairof BT BT))))")(scope)
    }
      
    intercept[UnboundVariableException] {
      bodyFor("(define-type <cross-procedure-type> (Rec T (-> (Pairof T T) *)))")(scope)
    }
  }
  
  test("defining type constructors") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    // No args (is this useful?)
    bodyFor("(define-type (Boolean) <boolean>)")(scope)
    bodyFor("(define-type <constructed-boolean> (Boolean))")(scope)

    assert(scope("<constructed-boolean>") === BoundType(vt.BooleanType))

    intercept[BadSpecialFormException] {
      // Too many arguments
      bodyFor("(define-type <too-many-args> (Boolean <pair>))")(scope)
    }

    // Non-symbol as argument
    intercept[BadSpecialFormException] {
      bodyFor("(define-type (NonSymbol 1) <boolean>)")(scope)
    }

    // Single arg
    bodyFor("(define-type (Option T) (U T <empty-list>))")(scope)
    bodyFor("(define-type <string-option> (Option <string>))")(scope)

    assert(scope("<string-option>") === BoundType(
      vt.UnionType(Set(
        vt.EmptyListType,
        vt.StringType
      ))
    ))

    intercept[TypeException] {
      // Violates the upper type bound
      bodyFor("(define-type (NumOption [T : <number>]) (U T <empty-list>))")(scope)
      bodyFor("(define-type <bad-option> (NumOption <string>))")(scope)
    }

    intercept[BadSpecialFormException] {
      // Not enough args
      bodyFor("(define-type <insufficient-args> (Option))")(scope)
    }

    intercept[BadSpecialFormException] {
      // Too many args
      bodyFor("(define-type <too-many-args> (Option <pair> <string>))")(scope)
    }

    // Multiple args and recursive types
    bodyFor("(define-type (ListWithTerminator M T) (Rec L (U T (Pairof M L))))")(scope)

    bodyFor("(define-type <string-unit-tlist> (ListWithTerminator <string> <unit>))")(scope)
    assert(scope("<string-unit-tlist>") === BoundType(
      vt.UnionType(Set(
        vt.UnitType,
        vt.SpecificPairType(
          vt.StringType,
          vt.RecursiveSchemeTypeRef(1)
        )
      ))
    ))

    // This is identical to a proper list
    bodyFor("(define-type <port-list> (ListWithTerminator <port> <empty-list>))")(scope)
    assert(scope("<port-list>") === BoundType(vt.UniformProperListType(vt.PortType)))

    // Nested type constructors
    bodyFor("(define-type <nested-type> (ListWithTerminator (Option <symbol>) <boolean>))")(scope)
    assert(scope("<nested-type>") === BoundType(
      vt.UnionType(Set(
        vt.BooleanType,
        vt.SpecificPairType(
          vt.UnionType(Set(
            vt.EmptyListType,
            vt.SymbolType
          )),
          vt.RecursiveSchemeTypeRef(1)
        )
      ))
    ))
  }
}
