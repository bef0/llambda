package io.llambda.compiler.planner.intermediatevalue
import io.llambda

import llambda.compiler.planner.PlanWriter
import llambda.compiler.planner.{step => ps}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.RuntimeErrorMessage
import llambda.compiler.ValueNotApplicableException

/** Trait for IntermediateValues that cannot be applied */
trait NonProcedureValue extends IntermediateValue {
  def toProcedureTempValue(
      targetType: vt.ProcedureType,
      errorMessageOpt: Option[RuntimeErrorMessage]
  )(implicit plan: PlanWriter): ps.TempValue = {
    val message = errorMessageOpt.map(_.text) getOrElse {
      s"Unable to convert ${typeDescription} to ${targetType}"
    }

    impossibleConversion(message)
  }

  def toProcedureValue()(implicit plan: PlanWriter): ProcedureValue =
    throw new ValueNotApplicableException(plan.activeContextLocated, typeDescription)
}

