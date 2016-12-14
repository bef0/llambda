package io.llambda.compiler
import io.llambda

sealed abstract class SemanticException(val located: SourceLocated, message: String) extends
    Exception(message + "\n" + located.locationString) {
  val semanticErrorType: String

  val errorCategory: ErrorCategory = ErrorCategory.Default
}

class DubiousLibraryNameComponentException(located: SourceLocated, val libraryName: String) extends SemanticException(located, libraryName) {
  val semanticErrorType = "dubious library name"
}

abstract class ReferencedFileNotFoundException(located: SourceLocated, val filename: String) extends SemanticException(located, filename)

class LibraryNotFoundException(located: SourceLocated, filename: String) extends ReferencedFileNotFoundException(located, filename) {
  val semanticErrorType = "library not found"
}

class IncludeNotFoundException(located: SourceLocated, filename: String) extends ReferencedFileNotFoundException(located, filename) {
  val semanticErrorType = "include not found"
}

class LibraryNameMismatchException(located: SourceLocated, val loadedName: Seq[String], val definedName: List[String]) extends
  SemanticException(located, "(" + loadedName.mkString(" ") + ") doesn't match (" + definedName.mkString(" ") + ")") {
  val semanticErrorType = "library name mismatch"
}

case class InvalidLibraryNameException(val datum: ast.Datum) extends SemanticException(datum, datum.toString) {
  val semanticErrorType = "invalid library name"
}

class NoSyntaxRuleException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "no syntax rule"
}

class MalformedExprException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "malformed expression"
}

class BadSpecialFormException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "bad special form"
}

class UnboundVariableException(located: SourceLocated, val variableName: String) extends SemanticException(located, variableName) {
  val semanticErrorType = "unbound variable"
}

class UserDefinedSyntaxError(located: SourceLocated, val errorString: String, val data: List[ast.Datum])
  extends SemanticException(located, errorString + " " + data.map(_.toString).mkString(" ")) {

  val semanticErrorType = "user defined syntax error"
}

class ImportedIdentifierNotFoundException(located: SourceLocated, val identifier: String) extends SemanticException(located, identifier) {
  val semanticErrorType = "imported identifier not found"
}

class ValueNotApplicableException(located: SourceLocated, typeDescription: String) extends SemanticException(located, s"${typeDescription.capitalize} not applicable") {
  val semanticErrorType = "not applicable"
}

class TypeException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "impossible type conversion"

  override val errorCategory = ErrorCategory.Type
}

class ArityException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "incompatible arity"

  override val errorCategory = ErrorCategory.Arity
}

class RangeException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "out of range"

  override val errorCategory = ErrorCategory.Range
}

class DivideByZeroException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "divide by zero"

  override val errorCategory = ErrorCategory.DivideByZero
}

class IntegerOverflowException(located: SourceLocated, message: String) extends SemanticException(located, message) {
  val semanticErrorType = "integer overflow"

  override val errorCategory = ErrorCategory.IntegerOverflow
}

class DefinitionOutsideTopLevelException(located: SourceLocated) extends BadSpecialFormException(located, "Definitions can only be introduced in at the outermost level or at the beginning of a body") 

class DuplicateDefinitionException(val symbol: sst.ScopedSymbol) extends SemanticException(symbol, s"Duplicate definition for ${symbol.name}") {
  val semanticErrorType = "duplicate define"
}
