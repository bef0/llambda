package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.{SourceLocated, ProcedureSignature, SourceProcedure}
import llambda.compiler.planner.{step => ps}

case class PlannedFunction(
  signature : ProcedureSignature,
  namedArguments : List[(String,  ps.TempValue)],
  steps : List[ps.Step],
  worldPtrOpt : Option[ps.WorldPtrValue],
  sourceProcedureOpt : Option[SourceProcedure]
) {
  /** Indicates if this procedure is internally generated by the compiler */
  val isArtificial =
    !sourceProcedureOpt.isDefined
}
