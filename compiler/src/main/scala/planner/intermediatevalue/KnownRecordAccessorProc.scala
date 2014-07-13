package io.llambda.compiler.planner.intermediatevalue
import io.llambda

import llambda.compiler.{ProcedureSignature, ContextLocated}
import llambda.compiler.planner._
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{step => ps}

class KnownRecordAccessorProc(recordType : vt.RecordType, field : vt.RecordField) extends KnownArtificialProc {
  protected val symbolHint =
    recordType.sourceName
      .replaceAllLiterally("<", "")
      .replaceAllLiterally(">", "") + 
      "-" + field.sourceName
        
  val signature = ProcedureSignature(
    hasWorldArg=false,
    hasSelfArg=false,
    hasRestArg=false,
    fixedArgs=List(recordType),
    returnType=Some(field.fieldType),
    attributes=Set()
  )
  
  def planFunction(parentPlan : PlanWriter) : PlannedFunction = {
    val recordCellTemp = ps.RecordTemp()
    
    val plan = parentPlan.forkPlan()

    // Extract the record data
    val recordDataTemp = ps.RecordLikeDataTemp()
    plan.steps += ps.LoadRecordLikeData(recordDataTemp, recordCellTemp, recordType) 
    
    // Read the field
    val fieldValueTemp = ps.Temp(field.fieldType)
    plan.steps += ps.LoadRecordDataField(fieldValueTemp, recordDataTemp, recordType, field) 
    plan.steps += ps.Return(Some(fieldValueTemp))

    PlannedFunction(
      signature=signature,
      namedArguments=List(("recordCell" -> recordCellTemp)),
      steps=plan.steps.toList,
      worldPtrOpt=None,
      debugContextOpt=None
    )
  }

  override def attemptInlineApplication(state : PlannerState)(operands : List[(ContextLocated, IntermediateValue)])(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : Option[PlanResult] = {
    operands match {
      case List((_, recordValue)) =>
        val recordCellTemp = recordValue.toTempValue(recordType)

        val recordDataTemp = ps.RecordLikeDataTemp()
        plan.steps += ps.LoadRecordLikeData(recordDataTemp, recordCellTemp, recordType) 
        
        // Read the field
        val fieldValueTemp = ps.Temp(field.fieldType)
        plan.steps += ps.LoadRecordDataField(fieldValueTemp, recordDataTemp, recordType, field) 

        Some(PlanResult(
          state=state,
          value=TempValueToIntermediate(field.fieldType, fieldValueTemp)
        ))

      case _ =>
        None
    }
  }
}