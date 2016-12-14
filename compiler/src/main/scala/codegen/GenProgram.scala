package io.llambda.compiler.codegen
import io.llambda

import scala.io.Source

import llambda.compiler._
import llambda.llvmir._
import llambda.compiler.platform.TargetPlatform
import llambda.compiler.planner.{step => ps}
import llambda.compiler.{celltype => ct}

object GenProgram {
  def resourceAsString(resourcePath: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(resourcePath)
    Source.fromInputStream(stream).mkString
  }

  def preludeIr: String = {
    List(
      RuntimeBuildFiles.llvmTarget,
      resourceAsString("generated/cellTypes.ll"),
      resourceAsString("defines.ll")
    ) mkString "\n"
  }

  def apply(functions: Map[String, planner.PlannedFunction], compileConfig: CompileConfig, entryFilenameOpt: Option[String]): String = {
    val module = new IrModuleBuilder
    module.metadataIndexSource.nextIndex = ct.CellType.nextMetadataIndex

    // Identify ourselves in our generated IR
    val compilerIdentifier = FeatureIdentifiers.compilerVersionIdentifier + " (based on LLVM)"
    module.identifyCompiler(compilerIdentifier)

    val plannedSymbols = functions.keySet
    val generatedTypes = BuildRecordLikeTypes(module, functions, compileConfig.targetPlatform)
    val constantGenerator = new ConstantGenerator(generatedTypes)

    // Package up our global generator state
    val genGlobals = GenGlobals(
      plannedSymbols=plannedSymbols,
      generatedTypes=generatedTypes,
      constantGenerator=constantGenerator,
      targetPlatform=compileConfig.targetPlatform
    )

    // Build each program-supplied function
    val functionGenerator = GenFunction(module, genGlobals)_

    for((nativeSymbol, plannedFunction) <- functions) {
      functionGenerator(nativeSymbol, plannedFunction)
    }

    // Build our main() glue to init the runtime and call our program
    val result = IrFunction.Result(IntegerType(32))
    val namedArguments = List(
      "argc" -> IrFunction.Argument(IntegerType(32)),
      "argv" -> IrFunction.Argument(PointerType(PointerType(IntegerType(8))))
    )

    val mainFunction = new IrFunctionBuilder(
      module=module,
      result=result,
      namedArguments=namedArguments,
      name="main"
    )

    val entryBlock = mainFunction.entryBlock

    // Initialize our runtime
    val runDecl = RuntimeFunctions.run

    module.declareFunction(runDecl)

    // Call __llambda_top_level through llcore_run
    // __llambda_top_level must be defined by the planner
    val execValue = GenNamedEntryPoint(module)(LlambdaTopLevelSignature, LlambdaTopLevelSignature.nativeSymbol, plannedSymbols)
    val skipFinal = IntegerConstant(IntegerType(1), 1)

    val runArgs = List(execValue, mainFunction.argumentValues("argc"), mainFunction.argumentValues("argv"), skipFinal)

    entryBlock.callDecl(None)(runDecl, runArgs)

    // Return 0
    // Scheme can only return non-zero exit codes using (exit)
    entryBlock.ret(IntegerConstant(IntegerType(32), 0))

    module.defineFunction(mainFunction)

    // Convert our IR to one big string
    preludeIr + "\n" + module.toIr + "\n"
  }
}

