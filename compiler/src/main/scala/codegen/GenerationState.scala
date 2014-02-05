package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.{StorageLocation, InternalCompilerErrorException}
import llambda.llvmir.{IrModuleBuilder, IrBlockBuilder, IrValue}
import llambda.compiler.planner.{step => ps}


case class GenerationState(
  module : IrModuleBuilder,
  gcSlots : GcSlotGenerator,
  currentBlock : IrBlockBuilder,
  currentAllocation : CellAllocation,
  liveTemps : Map[ps.TempValue, IrValue] = Map(),
  gcRootedTemps : Set[ps.TempValue] = Set()
) {
  def withTempValue(tempValue : (ps.TempValue, IrValue)) =
    this.copy(liveTemps=(liveTemps + tempValue))
}
