package io.llambda.compiler.frontend
import io.llambda

import llambda.compiler._

object ExtractCaseLambda {
  def apply(located: SourceLocated, clauseData: List[sst.ScopedDatum])(implicit context: FrontendContext): et.CaseLambda = {
    val locatedClauses = clauseData map {
      case clauseDatum @ sst.ProperList(sst.ListOrDatum(fixedArgData, restArgDatum) :: definition) =>
        val lambdaExpr = ExtractLambda(
          located=clauseDatum,
          argList=fixedArgData,
          argTerminator=restArgDatum,
          definition=definition
        ).assignLocationFrom(clauseDatum)

        (clauseDatum, lambdaExpr)

      case otherDatum =>
        throw new BadSpecialFormException(otherDatum, "Invalid (case-lambda) clause")
    }

    val locatedSignatures = locatedClauses map { case (located, lambdaExpr) =>
      (located, lambdaExpr.schemeType)
    }

    ValidateCaseLambdaClauses(locatedSignatures)
    et.CaseLambda(locatedClauses.map(_._2))
  }
}
