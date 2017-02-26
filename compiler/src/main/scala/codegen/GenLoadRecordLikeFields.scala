package io.llambda.compiler.codegen
import io.llambda

import llambda.llvmir._
import llambda.compiler.{valuetype => vt}

object GenLoadRecordLikeFields {
  def apply(block: IrBlockBuilder)(
      recordDataIr: IrValue,
      generatedType: GeneratedType,
      fieldsToLoad: List[vt.RecordField]
  ): List[IrValue] = fieldsToLoad.map { recordField =>
    val fieldIndices = generatedType.fieldToGepIndices(recordField)
    val fieldType = generatedType.recordLikeType.typeForField(recordField)
    val fieldIrType = ValueTypeToIr(fieldType).irType

    // Find the TBAA node
    val tbaaNode = generatedType.fieldToTbaaNode(recordField)

    // Get the element pointer
    val fieldPtr = block.getelementptr("fieldPtr")(fieldIrType, recordDataIr, (0 :: fieldIndices).map(IntegerConstant(IntegerType(32), _)))

    // Perform the load
    block.load("loadedField")(fieldPtr, metadata=Map("tbaa" -> tbaaNode))
  }
}
