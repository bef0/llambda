package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.{celltype => ct}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{intermediatevalue => iv}
import llambda.compiler.planner.{step => ps}

private[planner] object ValuesToList {
  /** Converts a list of intermediate values to an intermediate value representing a list
    *
    * This automatically takes advantage of immutable pair support to build constant lists
    *
    * @param  memberValues  Values to include in the proper list
    * @param  tailValue     Value for the tail of the list. To create a proper list pass iv.EmptyListValue or another
    *                       proper list.
    */
  def apply(
      memberValues: List[iv.IntermediateValue],
      tailValue: iv.IntermediateValue = iv.EmptyListValue
  )(implicit plan: PlanWriter): iv.IntermediateValue =
    memberValues.foldRight(tailValue) { case(carValue, cdrValue) =>
      val listLengthOpt = cdrValue match {
        case knownListElement: iv.KnownListElement =>
          knownListElement.listLengthOpt.map(_ + 1)

        case _ =>
          None
      }

      ValuesToPair(carValue, cdrValue, listLengthOpt)
    }
}
