package io.llambda.compiler.valuetype.polymorphic
import io.llambda

import org.scalatest.FunSuite

import llambda.compiler.valuetype._
import llambda.compiler.NoSourceLocation
import llambda.compiler.ImpossibleTypeConversionException

import Implicits._

class InstantiateTypeSuite extends FunSuite {
  val polyA = new TypeVar("A")
  val polyB = new TypeVar("B")

  test("instantiating a native type") {
    val polyNative = Int64
    val reconciled = ReconcileTypeVars.Result(Map())
    val expected = Int64

    assert(InstantiateType(reconciled, polyNative) === expected)
  }

  test("instantiating a leaf Scheme type") {
    val polyLeaf = ExactIntegerType
    val reconciled = ReconcileTypeVars.Result(Map())
    val expected = ExactIntegerType

    assert(InstantiateType(reconciled, polyLeaf) === expected)
  }

  test("instantiating a bare type var") {
    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType
    ))

    val expected = ExactIntegerType

    assert(InstantiateType(reconciled, polyA) == expected)
  }

  test("instantiating a pair type") {
    val polyPair = SpecificPairType(polyA, polyB)

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType,
      polyB -> FlonumType
    ))

    val expected = SpecificPairType(
      ExactIntegerType,
      FlonumType
    )

    assert(InstantiateType(reconciled, polyPair) == expected)
  }

  test("instantiating a nested pair") {
    val polyPair = SpecificPairType(
      polyA,
      SpecificPairType(
        polyA,
        polyA
      )
    )

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType
    ))

    val expected = SpecificPairType(
      ExactIntegerType,
      SpecificPairType(
        ExactIntegerType,
        ExactIntegerType
      )
    )

    assert(InstantiateType(reconciled, polyPair) == expected)
  }

  test("instantiating a union") {
    val polyUnion = UnionType(Set(polyA, BooleanType))

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType
    ))

    val expected = UnionType(Set(
      ExactIntegerType,
      BooleanType
    ))

    assert(InstantiateType(reconciled, polyUnion) == expected)
  }

  test("instantiating a uniform vector") {
    val polyVec = UniformVectorType(polyA)

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType
    ))

    val expected = UniformVectorType(
      ExactIntegerType
    )

    assert(InstantiateType(reconciled, polyVec) == expected)
  }

  test("instantiating a specific vector") {
    val polyVec = SpecificVectorType(Vector(polyA, polyB, polyA))

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType,
      polyB -> FlonumType
    ))

    val expected = SpecificVectorType(Vector(
      ExactIntegerType,
      FlonumType,
      ExactIntegerType
    ))

    assert(InstantiateType(reconciled, polyVec) == expected)
  }

  test("instantiating a uniform vector of pairs") {
    val polyVec = UniformVectorType(
      SpecificPairType(polyB, polyA)
    )

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType,
      polyB -> FlonumType
    ))

    val expected = UniformVectorType(
      SpecificPairType(
        FlonumType,
        ExactIntegerType
      )
    )

    assert(InstantiateType(reconciled, polyVec) == expected)
  }

  test("instantiating a procedure type") {
    val polyProc = ProcedureType(
      fixedArgTypes=List(polyA, PortType),
      restArgMemberTypeOpt=Some(polyB),
      returnType=ReturnType.SpecificValues(List(polyA, StringType))
    )

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType,
      polyB -> FlonumType
    ))

    val expected = ProcedureType(
      fixedArgTypes=List(ExactIntegerType, PortType),
      restArgMemberTypeOpt=Some(FlonumType),
      returnType=ReturnType.SpecificValues(List(ExactIntegerType, StringType))
    )

    assert(InstantiateType(reconciled, polyProc) == expected)
  }

  test("instantiating a case procedure type") {
    val polyCaseProc = CaseProcedureType(List(
      ProcedureType(
        fixedArgTypes=List(polyA),
        restArgMemberTypeOpt=None,
        returnType=ReturnType.SingleValue(polyB)
      ),
      ProcedureType(
        fixedArgTypes=List(polyA, polyB),
        restArgMemberTypeOpt=None,
        returnType=ReturnType.SingleValue(polyA)
      )
    ))

    val reconciled = ReconcileTypeVars.Result(Map(
      polyA -> ExactIntegerType,
      polyB -> FlonumType
    ))

    val expected = CaseProcedureType(List(
      ProcedureType(
        fixedArgTypes=List(ExactIntegerType),
        restArgMemberTypeOpt=None,
        returnType=ReturnType.SingleValue(FlonumType)
      ),
      ProcedureType(
        fixedArgTypes=List(ExactIntegerType, FlonumType),
        restArgMemberTypeOpt=None,
        returnType=ReturnType.SingleValue(ExactIntegerType)
      )
    ))

    assert(InstantiateType(reconciled, polyCaseProc) == expected)
  }

  test("instantiating a record instance") {
    val polyInner = new TypeVar("Inner")
    val recordType = new RecordType("<record-type>", Nil, typeVars=List(polyInner))

    val polyOuter = new TypeVar("Inner")

    val recordInstance = RecordTypeInstance(
      typeVars=ReconcileTypeVars.Result(Map(polyInner -> polyOuter)),
      recordType=recordType
    )

    val reconciled = ReconcileTypeVars.Result(Map(
      polyOuter -> ExactIntegerType
    ))

    val expected = RecordTypeInstance(
      typeVars=ReconcileTypeVars.Result(Map(polyInner -> ExactIntegerType)),
      recordType=recordType
    )

    assert(InstantiateType(reconciled, recordInstance) == expected)
  }
}
