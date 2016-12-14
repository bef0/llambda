package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.{valuetype => vt}

object CompactRepresentationForType extends (vt.ValueType => vt.ValueType) {
  /** Returns the most compact representation for the specified type
    *
    * Compact in this context refers to both the memory and garbage collector overhead of the type. This is typically an
    * unboxed value for types that have unboxed representations
    */
  def apply(valueType: vt.ValueType): vt.ValueType = valueType match {
    case vt.IntegerType => vt.Int64
    case vt.FlonumType => vt.Double
    case vt.BooleanType => vt.Predicate
    case vt.CharType => vt.UnicodeChar
    case other => other
  }
}
