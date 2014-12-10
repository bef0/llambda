package io.llambda.compiler.valuetype
import io.llambda

import org.scalatest.FunSuite

import llambda.compiler.{celltype => ct}
import llambda.compiler.valuetype.{polymorphic => pm}
import Implicits._

trait SchemeTypeSuite extends FunSuite {
  protected val recordAtomType = SchemeTypeAtom(ct.RecordCell)

  protected val recordInstance1 = RecordTypeInstance(
    pm.ReconcileTypeVars.Result(),
    new RecordType("record1", Nil)
  )

  protected val recordInstance2 = RecordTypeInstance(
    pm.ReconcileTypeVars.Result(),
    new RecordType("record2", Nil)
  )

  protected val literalTrue = LiteralBooleanType(true)
  protected val literalFalse = LiteralBooleanType(false)

  protected val stringList = UniformProperListType(StringType)
  protected val numericList = UniformProperListType(NumberType)
  protected val exactIntList = UniformProperListType(ExactIntegerType)
  protected val inexactList = UniformProperListType(FlonumType)
    
  protected val knownNumberList = PairType(NumberType,
    PairType(ExactIntegerType,
      PairType(FlonumType,
        EmptyListType)))
  
  protected def nonEmptyProperList(memberType : SchemeType) : SchemeType = 
    PairType(memberType, UniformProperListType(memberType))

  protected def assertIntersection(type1 : SchemeType, type2 : SchemeType, resultType : SchemeType) {
    assert((type1 & type2) === resultType) 
    assert((type2 & type1) === resultType) 
  }
}
