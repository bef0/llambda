package io.llambda.compiler.planner
import io.llambda

import org.scalatest.{FunSuite, Inside}

import llambda.compiler._
import llambda.compiler.frontend.IncludePath
import llambda.compiler.SchemeStringImplicits._


trait PlanHelpers extends FunSuite with Inside {
  val topLevelSymbol = codegen.LlambdaTopLevelSignature.nativeSymbol

  private def testPlanConfig(
      data: List[ast.Datum],
      optimise: Boolean,
      includePath: IncludePath = IncludePath(Nil)
  ) = {
    val featureIdentifiers = FeatureIdentifiers()

    val frontendConfig = frontend.FrontendConfig(
      includePath=includePath,
      featureIdentifiers=featureIdentifiers
    )

    val loader = new frontend.LibraryLoader
    val exprs = frontend.ExtractProgram(data)(loader, frontendConfig)
    val analysis = analyser.AnalyseExprs(exprs)

    planner.PlanConfig(
      optimise=optimise,
      analysis=analysis
    )
  }

  /** Returns a map of planned functions to the given Scheme data */
  protected def planForData(
      data: List[ast.Datum],
      optimise: Boolean,
      includePath: IncludePath = IncludePath(Nil)
  ): Map[String, PlannedFunction] = {
    val planConfig = testPlanConfig(data, optimise, includePath)
    planner.PlanProgram(planConfig.analysis.usedTopLevelExprs)(planConfig).functions
  }

  protected def nativeLibrariesFor(scheme: String): Set[NativeLibrary] = {
    val importDecl = datum"""(import (llambda base) (llambda nfi) (llambda typed) (llambda process-context))"""

    val data = importDecl :: SchemeParser.parseStringAsData(scheme)

    val planConfig = testPlanConfig(data, true)
    planner.PlanProgram(planConfig.analysis.usedTopLevelExprs)(planConfig).requiredNativeLibraries
  }
}
