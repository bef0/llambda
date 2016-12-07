package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.ProcedureSignature
import llambda.compiler.{valuetype => vt}
import llambda.compiler.codegen.CompactRepresentationForType

object ApplicableTypeToAdaptedSignature extends (vt.ApplicableType => ProcedureSignature) {
  /** Returns the expected signature for the specified applicable type
    *
    * This is used when creating procedure cells with a specific applicable type
    */
  def apply(applicableType : vt.ApplicableType) : ProcedureSignature =
    applicableType match {
      case procType : vt.ProcedureType =>
        val compactReturnType = procType.returnType match {
          case vt.ReturnType.SingleValue(schemeType) =>
            vt.ReturnType.SingleValue(CompactRepresentationForType(schemeType))

          case other =>
            other
        }

        ProcedureSignature(
          hasWorldArg=true,
          hasSelfArg=true,
          mandatoryArgTypes=procType.mandatoryArgTypes.map(CompactRepresentationForType),
          optionalArgTypes=procType.optionalArgTypes,
          restArgMemberTypeOpt=procType.restArgMemberTypeOpt,
          returnType=compactReturnType,
          attributes=Set()
        )

      case _ : vt.CaseProcedureType =>
        // All adapted case procedures have the same signature at the moment
        ProcedureSignature(
          hasWorldArg=true,
          hasSelfArg=true,
          mandatoryArgTypes=Nil,
          optionalArgTypes=Nil,
          restArgMemberTypeOpt=Some(vt.AnySchemeType),
          returnType=vt.ReturnType.SingleValue(vt.AnySchemeType),
          attributes=Set()
        )
    }
}
