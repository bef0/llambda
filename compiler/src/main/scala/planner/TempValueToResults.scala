package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.planner.{step => ps}
import llambda.compiler.{valuetype => vt}

object TempValueToResults {
  def apply(
      returnType : vt.ReturnType.ReturnType[vt.ValueType],
      resultTemp : ps.TempValue
  )(implicit plan : PlanWriter) : ResultValue =
    returnType match {
      case vt.ReturnType.UnreachableValue =>
        UnreachableValue

      case vt.ReturnType.SingleValue(valueType) =>
        val singleValue = TempValueToIntermediate(valueType, resultTemp)(plan.config)
        SingleValue(singleValue)
    }
}
