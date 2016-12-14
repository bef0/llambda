package io.llambda.compiler.planner
import io.llambda

/** Suffixes source names to ensure they're unique */
class SourceNameUniquer {
  private val nextSuffixForSourceName = new collection.mutable.HashMap[String, Int]()

  def reserve(sourceNames: String*): Unit =
    for(sourceName <- sourceNames)
      apply(sourceName)

  def apply(sourceName: String): String = {
    // Allocate another number
    val nextSuffix = nextSuffixForSourceName.getOrElse(sourceName, 1)
    nextSuffixForSourceName.put(sourceName, nextSuffix + 1)

    if (nextSuffix == 1) {
      // Use the original name
      return sourceName
    }
    else {
      // Suffix with -2, -3, etc
      return s"${sourceName}-${nextSuffix}"
    }
  }
}
