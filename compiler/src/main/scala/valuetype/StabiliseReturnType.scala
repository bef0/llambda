package io.llambda.compiler.valuetype


object StabiliseReturnType {
  /** Stablises the type of a return type
    *
    * This is a simple wrapper around [[StabliseType]] to work on [[ReturnType]] instancds
    */
  def apply(returnType: ReturnType.ReturnType[SchemeType]): ReturnType.ReturnType[SchemeType] = returnType match {
    case ReturnType.Unreachable =>
      ReturnType.Unreachable

    case ReturnType.Reachable(valueType) =>
      ReturnType.Reachable[SchemeType](StabiliseType(valueType))
  }
}
