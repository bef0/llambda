package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.InternalCompilerErrorException
import llambda.compiler.planner.{step => ps}
import llambda.compiler.{celltype => ct}
import llambda.llvmir._
import llambda.llvmir.IrFunction._

object GenParameterize {
  private val llibyDynamicenvPush = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_push",
    arguments=Nil,
    attributes=Set(NoUnwind)
  )

  private val llibyDynamicenvSetValue = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_set_value",
    arguments=List(
      Argument(PointerType(ct.ProcedureCell.irType)),
      Argument(PointerType(ct.DatumCell.irType))
    ),
    attributes=Set(NoUnwind)
  )
  
private val llibyDynamicenvPop = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_pop",
    arguments=Nil,
    attributes=Set(NoUnwind)
  )

  def apply(initialState : GenerationState, plannedSymbols : Set[String], typeGenerator : TypeGenerator)(step : ps.Parameterize) : GenerationState = step match {
    case ps.Parameterize(result, parameterValues, steps, innerResult) =>
      // Declare all support functions
      for(supportFunc <- List(llibyDynamicenvPush, llibyDynamicenvSetValue, llibyDynamicenvPop)) {
        initialState.module.unlessDeclared(supportFunc) {
          initialState.module.declareFunction(supportFunc)
        }
      }

      val initialBlock = initialState.currentBlock
      
      // Push the new environment
      initialBlock.callDecl(None)(llibyDynamicenvPush, Nil)

      // Set each value
      for((parameterTemp, valueTemp) <- step.parameterValues) {
        val parameterIr = initialState.liveTemps(parameterTemp) 
        val valueIr = initialState.liveTemps(valueTemp) 

        initialBlock.callDecl(None)(llibyDynamicenvSetValue, List(parameterIr, valueIr))
      }

      val finalState = GenPlanSteps(initialState, plannedSymbols, typeGenerator)(step.steps)
      val finalBlock = finalState.currentBlock

      // Pop the environment
      finalBlock.callDecl(None)(llibyDynamicenvPop, Nil)

      finalState.withTempValue(step.result -> finalState.liveTemps(step.innerResult)) 
  }
}
