package io.llambda.compiler.frontend
import io.llambda

import org.scalatest.FunSuite

import llambda.compiler._
import llambda.compiler.{valuetype => vt}

class NativeFunctionDeclSuite extends FunSuite with testutil.ExprHelpers {
  implicit val nfiScope = {
    new Scope(testutil.NfiExports() ++ typedLambdaBindings)
  }

  test("void native function") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UnitType),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <unit>))""")
    }
  }

  test("void native functioni with noreturn attribute") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UnitType),
        attributes=Set(ProcedureAttribute.NoReturn)
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <unit>) noreturn)""")
    }
  }

  test("function taking world pointer") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=true,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UnitType),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(world-function system-library "lliby_newline" (-> <unit>))""")
    }
  }

  test("function returning int8") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.Int8),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-int8>))""")
    }
  }

  test("function taking int16 and returning int32") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Int16),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.Int32),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-int16> <native-int32>))""")
    }
  }

  test("function taking world pointer, int64, float and returning double") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=true,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Int64, vt.Float),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.Double),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(world-function system-library "lliby_newline" (-> <native-int64> <native-float> <native-double>))""")
    }
  }

  test("function taking uint16 and returning uint32") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.UInt16),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UInt32),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-uint16> <native-uint32>))""")
    }
  }

  test("function taking bool and returning bool") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Predicate),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.Predicate),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-bool> <native-bool>))""")
    }
  }

  test("function taking int8 and returning unicode char") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Int8),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UnicodeChar),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-int8> <native-unicode-char>))""")
    }
  }

  test("function taking double and returning arbitrary values") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Double),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.ArbitraryValues,
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <native-double> *))""")
    }
  }

  test("function taking union and returning specific values") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.UnionType(Set(vt.StringType, vt.SymbolType))),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SpecificValues(List(vt.StringType, vt.SymbolType)),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> (U <string> <symbol>) (Values <string> <symbol>)))""")
    }
  }

  test("function taking a integer cell and returning a rational cell") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.ExactIntegerType),
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.FlonumType),
        attributes=Set()
      ).toPolymorphic,
      "lliby_newline"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_newline" (-> <exact-integer> <flonum>))""")
    }
  }

  test("function with only rest arg") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=Some(vt.AnySchemeType),
        returnType=vt.ReturnType.SingleValue(vt.AnySchemeType),
        attributes=Set()
      ).toPolymorphic,
      "lliby_vector"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_vector" (-> <any> * <any>))""")
    }
  }

  test("function with fixed and rest args") {
    val expectedFunction = et.NativeFunction(
      NativeSystemLibrary,
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=List(vt.Predicate),
        restArgMemberTypeOpt=Some(vt.ExactIntegerType),
        returnType=vt.ReturnType.SingleValue(vt.Int32),
        attributes=Set()
      ).toPolymorphic,
      "lliby_misc"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function system-library "lliby_misc" (-> <native-bool> <exact-integer> * <native-int>))""")
    }
  }

  test("function in static library") {
    val expectedFunction = et.NativeFunction(
      NativeStaticLibrary("testlib"),
      ProcedureSignature(
        hasWorldArg=false,
        hasSelfArg=false,
        fixedArgTypes=Nil,
        restArgMemberTypeOpt=None,
        returnType=vt.ReturnType.SingleValue(vt.UnitType),
        attributes=Set()
      ).toPolymorphic,
      "lliby_misc"
    )

    assertResult(expectedFunction) {
      exprFor("""(native-function (static-library "testlib") "lliby_misc" (-> <unit>))""")
    }
  }

  test("function with non-Scheme rest arg") {
    intercept[BadSpecialFormException] {
      exprFor("""(native-function system-library "lliby_vector" (-> <native-int64> * <any>))""")
    }
  }

  test("function returning unknown type") {
    intercept[UnboundVariableException] {
      exprFor("""(native-function system-library "lliby_newline" (-> <not-a-type>))""")
    }
  }

  test("function returning non-symbol") {
    intercept[BadSpecialFormException] {
      exprFor("""(native-function system-library "lliby_newline" (-> 4))""")
    }
  }

  test("function in unbound library") {
    intercept[UnboundVariableException] {
      exprFor("""(native-function unbound-library "lliby_newline" (-> <unit>))""")
    }
  }

  test("function in non-library") {
    intercept[BadSpecialFormException] {
      exprFor("""
        (native-function (native-function system-library "native_newline" ()) "lliby_newline" (-> <unit>))
      """)
    }
  }

  test("function taking unknown type") {
    intercept[UnboundVariableException] {
      exprFor("""(native-function system-library "lliby_newline" (-> <not-a-type> <unit>))""")
    }
  }

  test("function taking non-symbol") {
    intercept[BadSpecialFormException] {
      exprFor("""(native-function system-library "lliby_newline" (-> 4 <unit>))""")
    }
  }
}

