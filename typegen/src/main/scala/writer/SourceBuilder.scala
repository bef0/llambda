package io.llambda.typegen.writer

import collection.mutable.StringBuilder

/** Builds indented source strings for a C-like language */
abstract class SourceBuilder {
  protected val indentString: String
  protected def buildBlockStart()
  protected def buildBlockEnd()

  protected var indentLevel: Int = 0
  protected var sourceString = new StringBuilder
  protected var shouldSep: Boolean = false

  /** Add a new line to the source string
    *
    * The line will be automatically indented to the appropriate level
    */
  def +=(line: String) {
    appendRaw((indentString * indentLevel) + line + "\n")
  }

  /** Appends multiple lines separated by the specified string
    *
    * This is useful for creating lists where each value is a separate line.
    */
  def appendLines(lines: Iterable[String], separator: String) {
    if (!lines.isEmpty) {
      val indentPrefix = indentString * indentLevel

      appendRaw(indentPrefix + lines.mkString(separator + "\n" + indentPrefix) + "\n")
    }
  }

  /** Directly includes a string in the source
    *
    * This does not indent or add a trailing newline
    */
  def appendRaw(str: String) {
    shouldSep = true
    sourceString ++= str
  }

  /** Increases the current indent level by the specified amount */
  def indent(levels: Int = 1) {
    indentLevel = indentLevel + levels
  }

  /** Decreases the current indent level by the specified amount */
  def outdent(levels: Int = 1) {
    indentLevel = indentLevel - levels
  }

  /** Runs a block with an increased indent level
    *
    * indent() is called before the block is run and outdent() once it completes
    */
  def indented[T](innerBlock: => T): T = {
    indent(1)
    val result = innerBlock
    outdent(1)

    result
  }

  /** Starts a new block in the target language
    *
    * This will output the appropriate braces and increase the indent level
    * for the duration of the passed Scala block
    */
  def block[T](innerBlock: => T): T = {
    buildBlockStart()

    indentLevel = indentLevel + 1
    val result = innerBlock
    indentLevel = indentLevel - 1

    // We don't need a sep at the end of the block
    trimExtraTerminalSep()

    buildBlockEnd()
    result
  }

  /** Starts a new block in the target language followed by a separator
    *
    * This is shorthand for block + sep
    */
  def blockSep[T](innerBlock: => T): T = {
    val result = block(innerBlock)
    sep()
    result
  }

  /** Adds a blank line to the source strings
    *
    * Multiple calls to sep() will only introduce a single blank line.
    */
  def sep() {
    if (shouldSep) {
      sourceString ++= "\n"
      shouldSep = false
    }
  }

  protected def trimExtraTerminalSep() {
    if (sourceString.endsWith("\n\n")) {
      sourceString = sourceString.dropRight(1)
    }
  }

  override def toString: String =
    sourceString.toString
}
