package io.llambda.compiler
import io.llambda

import llambda.compiler.{valuetype => vt}
import llambda.compiler.valuetype.{polymorphic => pm}

case class PolymorphicSignature(
    typeVars: Set[pm.TypeVar],
    template: ProcedureSignature
) {
  private def instantiate(reconciled: pm.ReconcileTypeVars.Result): ProcedureSignature = {
    template.copy(
      mandatoryArgTypes=template.mandatoryArgTypes.map(pm.InstantiateType(reconciled, _)),
      optionalArgTypes=template.optionalArgTypes.map(pm.InstantiateType(reconciled, _)),
      restArgMemberTypeOpt=template.restArgMemberTypeOpt.map(pm.InstantiateType(reconciled, _)),
      returnType=pm.InstantiateType.instantiateReturnType(reconciled, template.returnType)
    )
  }

  /** Returns our signature for the given arguments */
  def signatureForArgs(located: SourceLocated, args: List[vt.SchemeType]): ProcedureSignature = {
    if (typeVars.isEmpty) {
      // Skip!
      return template
    }

    val mandatoryArgResults = (template.mandatoryArgTypes zip args) map { case (polyArg, evidenceArg) =>
      pm.ResolveTypeVars(typeVars, polyArg, evidenceArg)
    }

    val argsWithoutMandatory = args.drop(template.mandatoryArgTypes.length)

    val optionalArgResults = (template.optionalArgTypes zip argsWithoutMandatory) map { case (polyArg, evidenceArg) =>
      pm.ResolveTypeVars(typeVars, polyArg, evidenceArg)
    }

    val argsWithoutFixed = argsWithoutMandatory.drop(template.optionalArgTypes.length)

    val restArgResults = template.restArgMemberTypeOpt match {
      case Some(polyMemberType) =>
        argsWithoutFixed map { evidenceMemberType =>
          pm.ResolveTypeVars(typeVars, polyMemberType, evidenceMemberType)
        }

      case None =>
        Nil
    }

    val allResults = mandatoryArgResults ++ optionalArgResults ++ restArgResults
    val reducedResult = allResults.foldLeft(pm.ResolveTypeVars.Result())(_ ++ _)

    // Reconcile our type vars with their upper bounds
    val reconciled = pm.ReconcileTypeVars(typeVars, reducedResult, fixProcedure=true)
    instantiate(reconciled)
  }

  /** Returns our signature from setting all of our type variables to their upper type bounds
    *
    * For polymorphic native functions this represents the actual signature of the procedure
    */
  lazy val upperBound: ProcedureSignature = {
    // Use our upper type bounds for everything
    val reconciled = pm.ReconcileTypeVars(typeVars)
    instantiate(reconciled)
  }

  /** Returns the Scheme type for this polymorphic signature
    *
    * This is shorthand for upperBound.toSchemeProcedureType
    */
  def toSchemeProcedureType = upperBound.toSchemeProcedureType

  def toPolymorphicProcedureType: pm.PolymorphicProcedureType =
    pm.PolymorphicProcedureType(typeVars, template.toSchemeProcedureType)
}
