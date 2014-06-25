package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.InternalCompilerErrorException
import llambda.compiler.ProcedureAttribute
import llambda.compiler.planner.{step => ps}
import llambda.llvmir._
import llambda.compiler.{celltype => ct}

object GenPlanStep {
  // Our runtime doesn't have defined wrap behaviour so we don't either
  private val integerWrapBehaviour = Set(
    WrapBehaviour.NoSignedWrap,
    WrapBehaviour.NoUnsignedWrap
  ) : Set[WrapBehaviour]

  private def containsAllocatingStep(step : ps.Step) : Boolean = {
    step match {
      case allocating if step.canAllocate => true

      case cond : ps.CondBranch =>
        cond.innerBranches.flatMap(_._1).exists(containsAllocatingStep)

      case _ =>
        false
    }
  }

  private def stepCompareCondToIr(stepCond : ps.CompareCond.CompareCond) : ComparisonCond.ComparisonCond = stepCond match {
    case ps.CompareCond.Equal => ComparisonCond.Equal
    case ps.CompareCond.NotEqual => ComparisonCond.NotEqual
    case ps.CompareCond.GreaterThan => ComparisonCond.GreaterThan
    case ps.CompareCond.GreaterThanEqual => ComparisonCond.GreaterThanEqual
    case ps.CompareCond.LessThan => ComparisonCond.LessThan
    case ps.CompareCond.LessThanEqual => ComparisonCond.LessThanEqual
  }
  
  def apply(state : GenerationState, genGlobals : GenGlobals)(step : ps.Step) : GenResult = {
    genGlobals.debugInfoGeneratorOpt match {
      case Some(debugInfoGenerator) =>
        // Add our line numbering debug info if possible
        val debugMetadata = debugInfoGenerator.metadataForContextLocated(step)

        state.currentBlock.function.withMetadata(debugMetadata) {
          genStep(state, genGlobals)(step)
        }

      case None =>
        genStep(state, genGlobals)(step)
    }
  }

  private def genStep(state : GenerationState, genGlobals : GenGlobals)(step : ps.Step) : GenResult = step match {
    case ps.AllocateCells(worldPtrTemp, count) =>
      if (!state.currentAllocation.isEmpty) {
        // This is not only wasteful but dangerous as the previous allocation won't be fully initialized
        throw new InternalCompilerErrorException("Attempted cell allocation without fully consuming previous allocation")
      }

      val worldPtrIr = state.liveTemps(worldPtrTemp)

      val (allocState, allocation) = GenCellAllocation.genAllocation(state)(worldPtrIr, count)
      allocState.copy(currentAllocation=allocation)

    case createConstantStep : ps.CreateConstant =>
      val irResult = GenConstant(state, genGlobals.typeGenerator)(createConstantStep)

      state.withTempValue(createConstantStep.result -> irResult)

    case unboxValueStep : ps.UnboxValue =>
      val irBoxed = state.liveTemps(unboxValueStep.boxed)
      val irResult = GenUnboxing(state)(unboxValueStep, irBoxed)

      state.withTempValue(unboxValueStep.result -> irResult)
    
    case boxValueStep : ps.BoxValue =>
      val irUnboxed = state.liveTemps(boxValueStep.unboxed)
      val (boxState, irResult) = GenBoxing(state)(boxValueStep, irUnboxed)

      boxState.withTempValue(boxValueStep.result -> irResult)

    case ps.CreateNamedEntryPoint(resultTemp, signature, nativeSymbol) =>
      val irModule = state.currentBlock.function.module
      val irValue = GenNamedEntryPoint(irModule)(signature, nativeSymbol, genGlobals.plannedSymbols)

      state.withTempValue((resultTemp -> irValue))

    case ps.CastCellToTypeUnchecked(resultTemp, subvalueTemp, targetType) =>
      val subvalueIr = state.liveTemps(subvalueTemp)

      val irValue = subvalueIr match {
        case constant : IrConstant =>
          // We can use an anonymous constant bitcast here
          BitcastToConstant(constant, PointerType(targetType.irType))

        case _ =>
          targetType.genPointerBitcast(state.currentBlock)(subvalueIr)
      }
      
      state.copy(
        liveTemps=state.liveTemps.withAliasedTempValue(subvalueTemp, (resultTemp -> irValue))
      )

    case ps.ConvertNativeInteger(resultTemp, fromValueTemp, toBits, signed) =>
      val fromValueIr = state.liveTemps(fromValueTemp)

      val convertedIr = GenIntegerConversion(state.currentBlock)(fromValueIr, toBits, signed)
      
      state.withTempValue(resultTemp -> convertedIr)
    
    case ps.ConvertNativeIntegerToFloat(resultTemp, fromValueTemp, fromSigned, fpType) =>
      val fromValueIr = state.liveTemps(fromValueTemp)
      val resultIr = GenIntegerToFloatConversion(state.currentBlock)(fromValueIr, fromSigned, fpType)

      state.withTempValue(resultTemp -> resultIr)
    
    case ps.ConvertNativeFloat(resultTemp, fromValueTemp, fpType) =>
      val fromValueIr = state.liveTemps(fromValueTemp)
      val resultIr = GenFloatConversion(state.currentBlock)(fromValueIr, fpType)

      state.withTempValue(resultTemp -> resultIr)

    case ps.BuildProperList(resultTemp, valueTemps) =>
      val valueIrs = valueTemps.map(state.liveTemps.apply)

      val (listState, properListIr) = GenProperList(state)(valueIrs)

      listState.withTempValue(resultTemp -> properListIr)
    
    case ps.CalcProperListLength(resultTemp, listHeadTemp) =>
      val listHeadIr = state.liveTemps(listHeadTemp)
      val (listState, lengthIr) = GenCalcProperListLength(state)(listHeadIr)

      listState.withTempValue(resultTemp -> lengthIr)

    case ps.TestCellType(resultTemp, cellTemp, cellType, possibleTypes) =>
      val cellIr = state.liveTemps(cellTemp)

      // Build range metadata from our possible types
      val rangeMetadata = RangeMetadata.fromPossibleValues(
        integerType=ct.DatumCell.typeIdIrType,
        possibleTypes.map(_.typeId)
      )
      val loadMetadata = Map("range" -> rangeMetadata)

      // Load the type ID
      val block = state.currentBlock
      val datumIr = ct.DatumCell.genPointerBitcast(block)(cellIr)
      val typeIdIr = ct.DatumCell.genLoadFromTypeId(block)(datumIr, metadata=loadMetadata)

      val resultIr = block.icmp(cellType.llvmName + "Check")(ComparisonCond.Equal, None, typeIdIr, IntegerConstant(ct.DatumCell.typeIdIrType, cellType.typeId))

      state.withTempValue(resultTemp -> resultIr)

    case ps.CondBranch(resultTemp, testTemp, trueSteps, trueTemp, falseSteps, falseTemp) =>
      val testIr = state.liveTemps(testTemp)
      
      // Make two blocks
      val irFunction = state.currentBlock.function
      val trueStartBlock = irFunction.startChildBlock("condTrue")
      val falseStartBlock = irFunction.startChildBlock("condFalse")

      val postFlushState  = if (containsAllocatingStep(step)) {
        // Flush our GC roots out
        // This is half a barrier - we write out any pending roots
        // This has two purposes:
        // 1) It prevents unflushed roots from building up in the main execution path while being repeatedly flushed in
        //    branches
        // 2) It's significantly easier to reason about and merge the GC states of the branches if the parent roots are
        //    all flushed
        GenGcBarrier.flushGcRoots(state)
      }
      else {
        state
      }

      // Branch!
      state.currentBlock.condBranch(testIr, trueStartBlock, falseStartBlock)

      // Continue generation down both branches after splitting our state
      val trueStartState = postFlushState.copy(
        currentBlock=trueStartBlock
      )

      val falseStartState = postFlushState.copy(
        currentBlock=falseStartBlock
      )

      val trueResult = GenPlanSteps(trueStartState, genGlobals)(trueSteps)
      val falseResult = GenPlanSteps(falseStartState, genGlobals)(falseSteps)

      (trueResult, falseResult) match {
        case (BlockTerminated(_), BlockTerminated(_)) =>
          // Both branches terminated
          BlockTerminated(
            // Even terminated blocks need a GC state so we can know which slots they allocated
            GcState.fromBranches(postFlushState.gcState, List(trueResult.gcState, falseResult.gcState))
          )

        case (trueEndState : GenerationState, BlockTerminated(_)) =>
          // Only true terminated
          trueEndState.copy(
            gcState=GcState.fromBranches(trueResult.gcState, List(falseResult.gcState))
          ).withTempValue(resultTemp -> trueEndState.liveTemps(trueTemp))
        
        case (BlockTerminated(_), falseEndState : GenerationState) =>
          // Only false terminated
          falseEndState.copy(
            gcState=GcState.fromBranches(falseResult.gcState, List(trueResult.gcState))
          ).withTempValue(resultTemp -> falseEndState.liveTemps(falseTemp))

        case (trueEndState : GenerationState, falseEndState : GenerationState) =>
          // Neither branch terminated - we need to phi

          // Get the IR values from either side
          val trueEndBlock = trueEndState.currentBlock
          val trueResultIrValue = trueEndState.liveTemps(trueTemp)

          val falseEndBlock = falseEndState.currentBlock
          val falseResultIrValue = falseEndState.liveTemps(falseTemp)

          // Make a final block
          val irFunction = postFlushState.currentBlock.function

          val phiBlock = irFunction.startChildBlock("condPhi")
          trueEndBlock.uncondBranch(phiBlock)
          falseEndBlock.uncondBranch(phiBlock)

          // Make the result phi
          val phiResultIr = phiBlock.phi("condPhiResult")(PhiSource(trueResultIrValue, trueEndBlock), PhiSource(falseResultIrValue, falseEndBlock))

          // Find our common temp values in sorted order to ensure we generate stable IR
          val sortedTrueLiveTemps = trueEndState.liveTemps.tempValueToIr.toSeq.sortBy(_._2.toIr).map(_._1)
          val sortedCommonLiveTemps = sortedTrueLiveTemps.filter(falseEndState.liveTemps.tempValueToIr.contains)

          // Phi any values that have diverged across branches
          val tempValueToIrUpdate = sortedCommonLiveTemps map { liveTemp =>
            val trueValueIrValue = trueEndState.liveTemps(liveTemp)
            val falseValueIrValue = falseEndState.liveTemps(liveTemp)
              
            if (trueValueIrValue == falseValueIrValue) {
              // This is the same in both branches which means it came from our original state
              (liveTemp -> trueValueIrValue)
            }
            else {
              // This has diverged due to e.g. GC having happened in one branch
              val phiValueIr = phiBlock.phi("condPhiValue")(PhiSource(trueValueIrValue, trueEndBlock), PhiSource(falseValueIrValue, falseEndBlock))
              (liveTemp -> phiValueIr)
            }
          }

          // Make sure we preserve pointer identities or else the identity count will explode
          postFlushState.copy(
            currentBlock=phiBlock,
            liveTemps=state.liveTemps.withUpdatedIrValues(tempValueToIrUpdate),
            gcState=GcState.fromBranches(postFlushState.gcState, List(trueEndState.gcState, falseEndState.gcState))
          ).withTempValue(resultTemp -> phiResultIr)
      }
      
    case invokeStep @ ps.Invoke(resultOpt, signature, funcPtrTemp, arguments) =>
      val irSignature = ProcedureSignatureToIr(signature)
      val irFuncPtr = state.liveTemps(funcPtrTemp)
      val irArguments = arguments.map { argument =>
        state.liveTemps(argument.tempValue)
      }

      // Dispose of arguments before our barrier
      // This prevents us from GC rooting them needlessly
      val disposedArgTemps = arguments.filter(_.dispose).map(_.tempValue).toSet

      val preBarrierState = state.copy(
        liveTemps=state.liveTemps -- disposedArgTemps
      )

      val callBlock = () => {  
        val irValue = preBarrierState.currentBlock.call(Some("ret"))(irSignature, irFuncPtr, irArguments)
        (preBarrierState.currentBlock, irValue)
      }

      val (finalState, irRetOpt) = if (invokeStep.canAllocate) {
        if (signature.attributes.contains(ProcedureAttribute.NoReturn)) {
          // This can't return - unroot all of our values and terminate the function
          return state.terminateFunction(() => {
            preBarrierState.currentBlock.call(None)(irSignature, irFuncPtr, irArguments)
            preBarrierState.currentBlock.unreachable
          })
        }
        else {
          // We need a GC barrier
          GenGcBarrier(preBarrierState) {
            val invokeBlock = preBarrierState.currentBlock
            val successBlock = invokeBlock.function.startChildBlock("invokeSuccess") 

            val irValue = invokeBlock.invoke(Some("invokeRet"))(
              signature=irSignature,
              functionPtr=irFuncPtr,
              arguments=irArguments,
              normalBlock=successBlock,
              exceptionBlock=preBarrierState.gcCleanUpBlockOpt.get
            )

            (successBlock, irValue)
          }
        }
      }
      else {
        // This call can't allocate or throw exceptions - skip the barrier and invoke 
        val irValue = preBarrierState.currentBlock.call(Some("ret"))(irSignature, irFuncPtr, irArguments)
        (preBarrierState, irValue)
      }

      resultOpt match {
        case Some(resultTemp) =>
          finalState.withTempValue(resultTemp -> irRetOpt.get)

        case None =>
          finalState
      }

    case ps.Return(None) =>
      state.terminateFunction(() => {
        state.currentBlock.retVoid()
      })
    
    case ps.Return(Some(returnValueTemp)) =>
      val irRetValue = state.liveTemps(returnValueTemp)

      state.terminateFunction(() => {
        state.currentBlock.ret(irRetValue)
      })

    case ps.LoadPairCar(resultTemp, pairTemp) =>
      val pairIr = state.liveTemps(pairTemp)
      val carIr = ct.PairCell.genLoadFromCar(state.currentBlock)(pairIr)

      state.withTempValue(resultTemp -> carIr)
    
    case ps.LoadPairCdr(resultTemp, pairTemp) =>
      val pairIr = state.liveTemps(pairTemp)
      val cdrIr = ct.PairCell.genLoadFromCdr(state.currentBlock)(pairIr)

      state.withTempValue(resultTemp -> cdrIr)
    
    case ps.LoadVectorLength(resultTemp, vectorTemp) =>
      val vectorIr = state.liveTemps(vectorTemp)
      val carIr = ct.VectorCell.genLoadFromLength(state.currentBlock)(vectorIr)

      state.withTempValue(resultTemp -> carIr)

    case ps.LoadProcedureEntryPoint(resultTemp, procTemp) =>
      val procIr = state.liveTemps(procTemp)
      val entryPoint = ct.ProcedureCell.genLoadFromEntryPoint(state.currentBlock)(procIr)

      state.withTempValue(resultTemp -> entryPoint)

    case initStep : ps.InitRecordLike  =>
      val (initedState, initedRecordLike) = GenInitRecordLike(state, genGlobals.typeGenerator)(initStep)

      initedState
        .withTempValue(initStep.cellResult -> initedRecordLike.recordCell)
        .withTempValue(initStep.dataResult -> initedRecordLike.recordData)
    
    case ps.TestRecordLikeClass(resultTemp, recordCellTemp, recordLikeType) => 
      val generatedType = genGlobals.typeGenerator(recordLikeType)

      val recordCellIr = state.liveTemps(recordCellTemp)
      val irResult = GenTestRecordLikeClass(state.currentBlock)(recordCellIr, generatedType)

      state.withTempValue(resultTemp -> irResult)
    
    case ps.AssertRecordLikeClass(worldPtrTemp, recordCellTemp, recordLikeType, errorMessage) => 
      val worldPtrIr = state.liveTemps(worldPtrTemp)
      val generatedType = genGlobals.typeGenerator(recordLikeType)
      
      // Start our branches
      val irFunction = state.currentBlock.function
      val fatalBlock = irFunction.startChildBlock("wrongRecordClass")
      val successBlock = irFunction.startChildBlock("correctRecordClass")

      GenErrorSignal(state.copy(currentBlock=fatalBlock))(worldPtrIr, errorMessage)

      // Branch if we're not of the right class
      val recordCellIr = state.liveTemps(recordCellTemp)
      val irResult = GenTestRecordLikeClass(state.currentBlock)(recordCellIr, generatedType)

      state.currentBlock.condBranch(irResult, successBlock, fatalBlock)

      // Continue with the successful block
      state.copy(currentBlock=successBlock)
  
    case ps.SetRecordLikeDefined(recordCellTemp, recordLikeType) => 
      val generatedType = genGlobals.typeGenerator(recordLikeType)
      
      // Mark ourselves as defined
      val recordCellIr = state.liveTemps(recordCellTemp)
      val cellType = generatedType.recordLikeType.cellType
      val irResult = cellType.genStoreToIsUndefined(state.currentBlock)(IntegerConstant(cellType.isUndefinedIrType, 0), recordCellIr)

      state

    case ps.AssertRecordLikeDefined(worldPtrTemp, recordCellTemp, recordLikeType, errorMessage) => 
      val worldPtrIr = state.liveTemps(worldPtrTemp)
      val generatedType = genGlobals.typeGenerator(recordLikeType)
      
      // Start our branches
      val irFunction = state.currentBlock.function
      val fatalBlock = irFunction.startChildBlock("recordIsUndefined")
      val successBlock = irFunction.startChildBlock("recordIsDefined")

      GenErrorSignal(state.copy(currentBlock=fatalBlock))(worldPtrIr, errorMessage)

      // Branch if we're defined
      val recordCellIr = state.liveTemps(recordCellTemp)
      val cellType = generatedType.recordLikeType.cellType

      val isUndefinedBool = cellType.genLoadFromIsUndefined(state.currentBlock)(recordCellIr)
      val isUndefinedPred = state.currentBlock.truncTo("undefinedPred")(isUndefinedBool, IntegerType(1))

      state.currentBlock.condBranch(isUndefinedPred, fatalBlock, successBlock)

      // Continue with the successful block
      state.copy(currentBlock=successBlock)

    case ps.SetRecordDataField(recordDataTemp, recordType, recordField, newValueTemp) =>
      val recordDataIr = state.liveTemps(recordDataTemp)
      val newValueIr = state.liveTemps(newValueTemp)
      val generatedType = genGlobals.typeGenerator(recordType)
  
      GenSetRecordDataField(state.currentBlock)(recordDataIr, generatedType, recordField, newValueIr)

      state
    
    case ps.LoadRecordDataField(resultTemp, recordDataTemp, recordLikeType, recordField) =>
      val recordDataIr = state.liveTemps(recordDataTemp)
      val generatedType = genGlobals.typeGenerator(recordLikeType)
  
      val resultIr = GenLoadRecordDataField(state.currentBlock)(recordDataIr, generatedType, recordField)

      state.withTempValue(resultTemp -> resultIr)

    case ps.LoadRecordLikeData(resultTemp, recordCellTemp, recordLikeType) =>
      val recordCellIr = state.liveTemps(recordCellTemp)
      val generatedType = genGlobals.typeGenerator(recordLikeType)

      val resultIr = GenLoadRecordLikeData(state.currentBlock)(recordCellIr, generatedType)

      state.withTempValue(resultTemp -> resultIr)
      
    case ps.SetProcedureEntryPoint(procedureCellTemp, entryPointTemp) =>
      val procedureCellIr = state.liveTemps(procedureCellTemp)
      val entryPointIr = state.liveTemps(entryPointTemp)

      // Store the entry point
      ct.ProcedureCell.genStoreToEntryPoint(state.currentBlock)(entryPointIr, procedureCellIr)

      state

    case ps.DisposeValue(disposedTemp) =>
      state.copy(
        liveTemps=state.liveTemps - disposedTemp
      )

    case pushDynamic : ps.PushDynamicState =>
      GenParameterize.genPush(state)(pushDynamic)
      state
    
    case popDynamic : ps.PopDynamicState =>
      GenParameterize.genPop(state)(popDynamic)
      state
    
    case ps.IntegerAdd(resultTemp, val1Temp, val2Temp) =>
      val val1Ir = state.liveTemps(val1Temp)
      val val2Ir = state.liveTemps(val2Temp)

      val resultIr = state.currentBlock.add("addResult")(integerWrapBehaviour, val1Ir, val2Ir)

      state.withTempValue(resultTemp -> resultIr)
    
    case ps.IntegerSub(resultTemp, val1Temp, val2Temp) =>
      val val1Ir = state.liveTemps(val1Temp)
      val val2Ir = state.liveTemps(val2Temp)

      val resultIr = state.currentBlock.sub("subResult")(integerWrapBehaviour, val1Ir, val2Ir)

      state.withTempValue(resultTemp -> resultIr)
    
    case ps.IntegerMul(resultTemp, val1Temp, val2Temp) =>
      val val1Ir = state.liveTemps(val1Temp)
      val val2Ir = state.liveTemps(val2Temp)

      val resultIr = state.currentBlock.mul("mulResult")(integerWrapBehaviour, val1Ir, val2Ir)

      state.withTempValue(resultTemp -> resultIr)

    case ps.IntegerCompare(resultTemp, compareCond, signed, val1Temp, val2Temp) =>
      val val1Ir = state.liveTemps(val1Temp)
      val val2Ir = state.liveTemps(val2Temp)

      val condIr = stepCompareCondToIr(compareCond)
      val resultIr = state.currentBlock.icmp("compResult")(condIr, signed, val1Ir, val2Ir)

      state.withTempValue(resultTemp -> resultIr)

    case ps.InitPair(resultTemp) =>
      val block = state.currentBlock
      val allocation = state.currentAllocation
      
      val (newAllocation, resultIr) = allocation.consumeCells(block)(1, ct.PairCell)
      
      state.copy(
        currentAllocation=newAllocation
      ).withTempValue(resultTemp -> resultIr)
    
    case ps.AssertPairMutable(worldPtrTemp, pairTemp, errorMessage) => 
      val worldPtrIr = state.liveTemps(worldPtrTemp)
      val pairIr = state.liveTemps(pairTemp)
      
      // Start our branches
      val irFunction = state.currentBlock.function
      val fatalBlock = irFunction.startChildBlock("pairIsImmutable")
      val successBlock = irFunction.startChildBlock("pairIsMutable")

      GenErrorSignal(state.copy(currentBlock=fatalBlock))(worldPtrIr, errorMessage)

      // The GC state can only be 0 (GC allocated) or 1 (global constant)
      // There are other GC states temporarily used during GC but they should never be reachable by program code
      val rangeMetadata = RangeMetadata(ct.DatumCell.gcStateIrType, (0, 2))

      val globalConstantGcState = IntegerConstant(ct.DatumCell.gcStateIrType, 1)
      val gcStateIr = ct.PairCell.genLoadFromGcState(state.currentBlock)(pairIr, metadata=Map("range" -> rangeMetadata))
      val irResult = state.currentBlock.icmp("isMutable")(ComparisonCond.NotEqual, None, globalConstantGcState, gcStateIr) 

      state.currentBlock.condBranch(irResult, successBlock, fatalBlock)

      // Continue with the successful block
      state.copy(currentBlock=successBlock)

    case ps.SetPairCar(pairTemp, newValueTemp) =>
      val pairIr = state.liveTemps(pairTemp)
      val newValueIr = state.liveTemps(newValueTemp)
      
      ct.PairCell.genStoreToCar(state.currentBlock)(newValueIr, pairIr)
      state

    case ps.SetPairCdr(pairTemp, newValueTemp) =>
      val pairIr = state.liveTemps(pairTemp)
      val newValueIr = state.liveTemps(newValueTemp)
      
      ct.PairCell.genStoreToCdr(state.currentBlock)(newValueIr, pairIr)
      state

    case ps.AssertPredicate(worldPtrTemp, predicateTemp, errorMessage) =>
      val worldPtrIr = state.liveTemps(worldPtrTemp)
      val predIr = state.liveTemps(predicateTemp)
      
      // Start our branches
      val irFunction = state.currentBlock.function
      val fatalBlock = irFunction.startChildBlock("predTrue")
      val successBlock = irFunction.startChildBlock("predFalse")

      GenErrorSignal(state.copy(currentBlock=fatalBlock))(worldPtrIr, errorMessage)

      state.currentBlock.condBranch(predIr, successBlock, fatalBlock)

      // Continue with the successful block
      state.copy(currentBlock=successBlock)
  
  }
}
