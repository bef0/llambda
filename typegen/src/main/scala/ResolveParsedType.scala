package io.llambda.typegen

import io.llambda.llvmir

object ResolveParsedType {
  def apply(fieldTypes : Map[String, FieldType])(parsedType : ParsedType) : FieldType = {
    val recursiveResolve = apply(fieldTypes)_

    parsedType match {
      case ParsedPointerType(pointeeType) =>
        PointerFieldType(recursiveResolve(pointeeType))

      case ParsedFunctionPointerType(returnType, arguments) =>
        val retType = returnType.map(recursiveResolve)
        FunctionPointerFieldType(retType, arguments.map(recursiveResolve))

      case parsedTypeName @ ParsedTypeName(name) =>
        // Look up the name from our already defined types
        fieldTypes.getOrElse(name, {
          throw new UnknownTypeException(parsedTypeName)
        })
    }
  }
}
