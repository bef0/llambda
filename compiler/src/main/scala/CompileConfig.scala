package io.llambda.compiler
import io.llambda

import llambda.compiler.frontend.{IncludePath, IncludeLoader}

case class CompileConfig(
  includePath : IncludePath,
  targetPlatform : platform.TargetPlatform,
  optimizeLevel : Int = 0,
  emitLlvm : Boolean = false,
  extraFeatureIdents : Set[String] = Set()
)