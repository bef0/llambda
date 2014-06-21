package io.llambda.compiler.reducer
import io.llambda

import llambda.compiler._
import llambda.compiler.reducer.{partialvalue => pv}
import llambda.compiler.{valuetype => vt}

private[reducer] object ReduceApplication {
  private val reportProcReducers = List[reportproc.ReportProcReducer](
    reportproc.ApplyProcReducer,
    reportproc.BooleanProcReducer,
    reportproc.CallCcProcReducer,
    reportproc.EquivalenceProcReducer,
    reportproc.NumberProcReducer,
    reportproc.ListProcReducer,
    reportproc.StringProcReducer,
    reportproc.SymbolProcReducer,
    reportproc.VectorProcReducer
  )
  
  private sealed abstract class ResolvedAppliedExpr {
    def toOutOfLine : ResolvedAppliedExpr
  }

  private case class ResolvedLambda(lambdaExpr : et.Lambda, inlineDefinition : Boolean) extends ResolvedAppliedExpr {
    def toOutOfLine = this.copy(inlineDefinition=false)
  }

  private case class ResolvedTypePredicate(schemeType : vt.SchemeType) extends ResolvedAppliedExpr {
    def toOutOfLine = this
  }

  private case class ResolvedReportProcedure(appliedVar : et.VarRef, reportProc : ReportProcedure) extends ResolvedAppliedExpr {
    def toOutOfLine = this
  }

  /** Returns if an expression is trivial
    *
    * "Trivial" is a qualitative property which means a negligible amount of code needs to be generated for the 
    * expression. At the moment that's either variable references or constant data that cannot reference other data
    * (leaf data).
    */
  private def exprIsTrivial(expr : et.Expr) : Boolean = expr match {
    case et.Literal(_ : ast.Leaf) | et.VarRef(_) =>
      true

    case _ =>
      false
  }
  
  /** Returns true if we should inline an inlineable expression */
  private def shouldInlineExpr(candidateExpr : et.Expr, allowNesting : Boolean = true) : Boolean = candidateExpr match {
    case et.InternalDefine(bindings, bodyExpr) =>
      if (bindings.map(_._2).forall(exprIsTrivial)) {
        // Unwrap the internal definition
        shouldInlineExpr(bodyExpr)
      }
      else {
        false
      }

    case et.Cond(testExpr, trueExpr, falseExpr) if allowNesting  =>
      // This catches (if (simple-application) trivial trivial)
      // Prevent nesting so we don't do this recursively 
      shouldInlineExpr(testExpr, allowNesting=false) && 
        exprIsTrivial(trueExpr) && 
        exprIsTrivial(falseExpr)

    case applyExpr : et.Apply =>
      applyExpr.subexprs.forall(exprIsTrivial)

    case otherExpr =>
      exprIsTrivial(otherExpr)
  }

  /** Reduces a lambda application via optional inlining 
    *
    * @param  located          Location of the application. This is used to generate accurate debug info.
    * @param  lambdaExpr       Lambda expression of the procedure being applied
    * @param  reducedOperands  Reduced expressions of the operands of the application in application order
    * @param  forceInline      If true inlining will always be performed if possible. This is intended to force
    *                          inlining for single-use procedures where bloating generated code isn't a concern.
    * @return Defined option for the expression to replace the application with or None if inlining should not be 
    *         performed
    */
  private def reduceLambdaApplication(located : ContextLocated, lambdaExpr : et.Lambda, reducedOperands : List[et.Expr], forceInline : Boolean)(reduceConfig : ReduceConfig) : Option[et.Expr] = {
    // Make sure we have the right arity
    if (lambdaExpr.restArg.isDefined) {
      if (reducedOperands.length < lambdaExpr.fixedArgs.length) {
        return None
      }
    }
    else if (reducedOperands.length != lambdaExpr.fixedArgs.length) {
      return None
    }

    if (lambdaExpr.fixedArgs.exists(reduceConfig.analysis.mutableVars.contains(_))) {
      // Not implemented
      return None
    }
    
    if (reduceConfig.inlineDepth > 5) {
      // Too deep
      return None
    }

    // Make new bindings for the fixed arguments
    // We can't create a full binding for the rest argument (yet) - we only set a known value
    val newBindings = lambdaExpr.fixedArgs.zip(reducedOperands)

    // Create partial values for all values we know
    val newFixedKnownValues = newBindings.map { case (storageLoc, operandExpr) =>
      val partialValue = pv.PartialValue.fromReducedExpr(operandExpr)

      if (PartialValueHasType(partialValue, storageLoc.schemeType) != Some(true)) {
        // We don't meet the type constraints or the partial value's type can't be determined
        return None
      }

      storageLoc -> partialValue
    }

    val newRestKnownValues = (lambdaExpr.restArg map { restArgLoc =>
      // Find our rest arguments (if any)
      val restReducedOperands = reducedOperands.drop(lambdaExpr.fixedArgs.length)
      val restPartialValues = restReducedOperands.map(pv.PartialValue.fromReducedExpr(_))

      // Make a partial proper list for the rest arg
      // This allows things like (length) on the rest args, allow it to be re-packed for (apply) etc.
      val restArgPartialList = pv.ProperList(restPartialValues)

      restArgLoc -> restArgPartialList
    }).toList
    
    // Make the analysis for the inner variables
    val analysis = reduceConfig.analysis

    val innerReduceConfig = reduceConfig.copy(
      knownValues=reduceConfig.knownValues ++ newFixedKnownValues ++ newRestKnownValues,
      inlineDepth=reduceConfig.inlineDepth + 1
    )

    // Reduce the expression
    val reducedBodyExpr = ReduceExpr(lambdaExpr.body)(innerReduceConfig)

    // Can we return?
    if (ExprCanReturn(reducedBodyExpr)) {
      // We can't safely inline this
      return None
    }

    // Do we reduce to a single variable reference?
    // This is technically a type of eta conversion and makes our ouput cleaner
    reducedBodyExpr match {
      case et.VarRef(storageLoc) =>
        for((bindingLoc, initializer) <- newBindings) {
          if (bindingLoc == storageLoc) {
            return Some(initializer)
          }
        }

      case _ =>
    }

    val bodyUsedVars = ReferencedVariables(reducedBodyExpr)
    
    // Is the rest arg itself still referenced?
    // We refuse to manually build a rest arg at the moment so we fall back to the planner to do so
    for(restArgStorageLoc <- lambdaExpr.restArg) {
      if (bodyUsedVars.contains(restArgStorageLoc)) {
        return None
      }
    }

    if (forceInline || shouldInlineExpr(reducedBodyExpr)) {
      // Annotate ourselves as inlined for debug info purposes
      val inlinePathEntry = InlinePathEntry(
        locationOpt=located.locationOpt,
        contextOpt=located.contextOpt,
        inlineReason=InlineReason.LambdaInline
      )
      val inlinedBodyExpr = reducedBodyExpr.asInlined(inlinePathEntry)

      // Figure out which bindings are still required
      val usedBindings = newBindings filter { case (argLoc, operandExpr) =>
        bodyUsedVars.contains(argLoc) || ExprHasSideEffects(operandExpr)
      }

      Some(if (usedBindings.isEmpty) {
        // No bindings, just the return the reduced body
        inlinedBodyExpr
      }
      else {
        // Wrap in a binding expression
        et.InternalDefine(usedBindings, inlinedBodyExpr)
      })
    }
    else {
      // Inlining is possible but determined not to be beneficial 
      None
    }
  }


  /** Resolves an applied expression attempting to find a report procedure or lambda */
  private def resolveAppliedExpr(expr : et.Expr, ignoreReportProcs : Set[ReportProcedure])(implicit reduceConfig : ReduceConfig) : Option[ResolvedAppliedExpr] = expr match {
    case appliedVar @ et.VarRef(reportProcedure : ReportProcedure) if !ignoreReportProcs.contains(reportProcedure) =>
      Some(ResolvedReportProcedure(appliedVar, reportProcedure))

    case et.VarRef(storageLoc) if reduceConfig.inlineDepth < 6 =>
      // Dereference the variable
      val storageLocExprOpt = (reduceConfig.knownValues.get(storageLoc).flatMap(_.toExprOpt) orElse
        reduceConfig.analysis.constantTopLevelBindings.get(storageLoc)
      )

      // Increase our inline depth in case we're dealing with an argument initialised with itself
      val innerReduceConfig = reduceConfig.copy(
        inlineDepth=reduceConfig.inlineDepth + 1
      )
          
      storageLocExprOpt.flatMap { derefedExpr =>
        // This is no longer an inline definition
        resolveAppliedExpr(derefedExpr, ignoreReportProcs)(innerReduceConfig).map(_.toOutOfLine)
      }

    case lambdaExpr : et.Lambda =>
      Some(ResolvedLambda(lambdaExpr, true))

    case et.TypePredicate(schemeType) =>
      Some(ResolvedTypePredicate(schemeType))

    case other => 
      None
  }

  /** Reduces a procedure application via optional inlining 
    *
    * @param  located            Location of the application. This is used to generate accurate debug info.
    * @param  appliedExpr        Expr being applied
    * @param  reducedOperands    Reduced expressions of the operands of the application in application order
    * @param  ignoreReportProcs  Set of report procedures to treat as normal values. This is used internally to retry
    *                            reduction after failing to do report procedure specific reduction.
    * @return Expr to replace the original application with or None to use the original expression
    */
  def apply(located : ContextLocated, appliedExpr : et.Expr, reducedOperands : List[et.Expr], ignoreReportProcs : Set[ReportProcedure] = Set())(implicit reduceConfig : ReduceConfig) : Option[et.Expr] = {
    resolveAppliedExpr(appliedExpr, ignoreReportProcs) flatMap {
      case ResolvedReportProcedure(appliedVar, reportProc) =>
        // Run this through the report procedure reducers
        for(reportProcReducer <- reportProcReducers) {
          for(expr <- reportProcReducer(appliedVar, reportProc.reportName, reducedOperands)) {
            // This was recognized and reduced - perform no further action
            return Some(expr)
          }
        }

        // Couldn't reduce as a report procedure; retry as a normal expression
        apply(located, appliedExpr, reducedOperands, ignoreReportProcs + reportProc)

      case ResolvedTypePredicate(schemeType) =>
        reducedOperands match {
          case List(testedExpr) =>
            for(testedValue <- PartialValueForExpr(testedExpr);
                hasType <- PartialValueHasType(testedValue, schemeType))
            yield
              et.Literal(ast.BooleanLiteral(hasType))

          case _ =>
            // Weird arity
            None
        }

      case ResolvedLambda(lambdaExpr, inlineDefinition) =>
        reduceLambdaApplication(located, lambdaExpr, reducedOperands, inlineDefinition)(reduceConfig)
    }
  }
}
