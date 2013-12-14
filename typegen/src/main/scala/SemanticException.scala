package io.llambda.typegen

import scala.util.parsing.input.Positional

sealed abstract class SemanticException(message : String) extends Exception(message) 

sealed abstract class PositionedSemanticException(val positional : Positional, message : String) extends 
  SemanticException(message + "\n" + positional.pos.longString) 

class DuplicateTypeNameException(val parsedDef : ParsedDefinition) extends
  PositionedSemanticException(parsedDef, s"Duplicate type name: ${parsedDef.name}")

class UndefinedCellClassException(errorPos : Positional, val cellClassName : String) extends
  PositionedSemanticException(errorPos, s"Undefined forward-declared cell class: ${cellClassName}")

class UnknownTypeException(val parsedTypeName : ParsedTypeName) extends
  PositionedSemanticException(parsedTypeName, s"Unknown type name: ${parsedTypeName.toString}")

class DuplicateRootCellClassException(val duplicateDecl : ParsedRootClassDefinition) extends
  PositionedSemanticException(duplicateDecl, s"Duplicate root cell class: ${duplicateDecl.name}")

class NoRootCellClassException extends SemanticException("No root cell class defined")

class DuplicateFieldNameException(val parsedCellField : ParsedCellField) extends
  PositionedSemanticException(parsedCellField, s"Duplicate field name: ${parsedCellField.name}")
