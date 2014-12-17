package io.llambda.compiler.functional
import io.llambda

import java.io.File
import java.io.{InputStream}
import scala.io.Source
import scala.sys.process._
import org.scalatest.{FunSuite, Inside}

import llambda.compiler._
import llambda.compiler.SchemeStringImplicits._

abstract class SchemeFunctionalTestRunner(testName : String, onlyOptimised : Boolean = false) extends FunSuite with Inside {
  // Implicit import decl every test gets
  private val testImportDecl = datum"(import (llambda nfi) (scheme base) (llambda test-util))"

  private val AbnormalExitCodes = List(
    // SIGILL
    128 + 4,
    // SIGABRT
    128 + 6,
    // SIGBUS
    128 + 10,
    // SIGSEGV
    128 + 11
  )

  private val targetPlatform = platform.DetectJvmPlatform()

  private case class ExecutionResult(success : Boolean, output : List[ast.Datum], errorString : String)

  val resourceBaseDir = "functional/"
  val resourceBaseUrl = getClass.getClassLoader.getResource(resourceBaseDir)
  val resourcePath = s"${resourceBaseDir}${testName}.scm"

  val includePath = frontend.IncludePath(
    fileParentDir=Some(resourceBaseUrl),
    packageRootDir=Some(resourceBaseUrl)
  )

  val stream = getClass.getClassLoader.getResourceAsStream(resourcePath)

  if (stream == null) {
    throw new Exception(s"Unable to load Scheme test source from ${resourcePath}")
  }

  // Load the tests
  val allTestSource = Source.fromInputStream(stream, "UTF-8").mkString

  val parsed = SchemeParser.parseStringAsData(allTestSource, Some(s":/${resourcePath}"))
  runAllTests(parsed)

  private def runAllTests(allTests : List[ast.Datum]) {
    if (!onlyOptimised) {
      // Just run one pass at -O 0
      runTestConfiguration(allTests, dialect.Dialect.default, 0)
    }

    // Run every dialect at -O 2
    for(dialect <- dialect.Dialect.dialects.values) {
      runTestConfiguration(allTests, dialect, 2)
    }
  }

  /** Expands top-level (cond-expand) expressions in the test source */
  private def expandTopLevel(data : List[ast.Datum])(implicit libraryLoader : frontend.LibraryLoader, frontendConfig : frontend.FrontendConfig) : List[ast.Datum] = {
    data flatMap {
      case ast.ProperList(ast.Symbol("cond-expand") :: firstClause :: restClauses) =>
        frontend.CondExpander.expandData(firstClause :: restClauses)

      case other =>
        List(other)
    }
  }

  private def runTestConfiguration(allTests : List[ast.Datum], schemeDialect : dialect.Dialect, optimizeLevel : Int) {
    // Deal with (cond-expand) for this configuration
    val expandLibraryLoader = new frontend.LibraryLoader(targetPlatform)
    val expandFrontendConfig = frontend.FrontendConfig(
      includePath=includePath,
      featureIdentifiers=targetPlatform.platformFeatures ++ schemeDialect.dialectFeatures,
      schemeDialect=schemeDialect
    )

    val expandedTests = expandTopLevel(allTests)(expandLibraryLoader, expandFrontendConfig)

    for(singleTest <- expandedTests) {
      singleTest match {
        case ast.ProperList(ast.Symbol("define-test") :: ast.StringLiteral(name) :: condition :: Nil) =>
          // Start a nested test
          test(s"$name (${schemeDialect.name} -O ${optimizeLevel})") {
            runSingleCondition(condition, schemeDialect, optimizeLevel)
          }

        case other =>
          fail("Unable to parse test: " + singleTest.toString)
      }
    }
  }

  private def runSingleCondition(condition : ast.Datum, schemeDialect : dialect.Dialect, optimizeLevel : Int) {
    condition match {
      case ast.ProperList(ast.Symbol("expect") :: expectedValue :: program) if !program.isEmpty =>
        val result = executeProgram(wrapForPrinting(program), schemeDialect, optimizeLevel)

        if (!result.success) {
          if (result.errorString.isEmpty) {
            fail("Execution unexpectedly failed with no output")
          }
          else {
            // Use the error string the program provided
            fail(result.errorString)
          }
        }

        assert(result.output === List(expectedValue))

      case ast.ProperList(ast.Symbol("expect-output") :: ast.ProperList(expectedOutput) :: program) if !program.isEmpty =>
        val result = executeProgram(program, schemeDialect, optimizeLevel)

        if (!result.success) {
          if (result.errorString.isEmpty) {
            fail("Execution unexpectedly failed with no output")
          }
          else {
            // Use the error string the program provided
            fail(result.errorString)
          }
        }

        assert(result.output === expectedOutput)

      case ast.ProperList(ast.Symbol("expect-success") :: program) if !program.isEmpty =>
        // Make sure the program outputs this at the end
        val canaryValue = ast.Symbol("test-completed")
        val programWithCanary = program :+ ast.ProperList(List(ast.Symbol("quote"), canaryValue))

        val result = executeProgram(wrapForPrinting(programWithCanary), schemeDialect, optimizeLevel)

        if (!result.success) {
          if (result.errorString.isEmpty) {
            fail("Execution unexpectedly failed with no output")
          }
          else {
            // Use the error string the program provided
            fail(result.errorString)
          }
        }

        assert(result.output === List(canaryValue), "Execution did not reach end of test")

      case ast.ProperList(ast.Symbol("expect-failure") :: program) if !program.isEmpty =>
        try {
          val result = executeProgram(program, schemeDialect, optimizeLevel)

          // If we compiled make sure we fail at runtime
          assert(result.success === false, "Execution unexpectedly succeeded")
        }
        catch {
          case e : SemanticException =>
            // Semantic exceptions are allowed
        }

      case ast.ProperList(ast.Symbol("expect-compile-failure") :: program) if !program.isEmpty =>
        intercept[SemanticException] {
          executeProgram(program, schemeDialect, optimizeLevel)
        }

      case ast.ProperList(ast.Symbol("expect-runtime-failure") :: program) if !program.isEmpty =>
        val result = executeProgram(program, schemeDialect, optimizeLevel)
        assert(result.success === false, "Execution unexpectedly succeeded")

      case ast.ProperList(ast.Symbol("expect-error") :: ast.Symbol(errorPredicate) :: program) if !program.isEmpty =>
        try {
          val wrappedProgram = wrapForAssertRaises(errorPredicate, program)
          val result = executeProgram(wrappedProgram, schemeDialect, optimizeLevel)

          if (!result.success) {
            if (result.errorString.isEmpty) {
              fail("Execution unexpectedly failed with no output")
            }
            else {
              fail(result.errorString)
            }
          }
        }
        catch {
          case expectedError : SemanticException
            if expectedError.errorCategory == ErrorCategory.fromPredicate(errorPredicate) =>
        }

      case other =>
          fail("Unable to parse condition: " + condition.toString)
    }
  }

  private def wrapForPrinting(program : List[ast.Datum]) : List[ast.Datum] = {
    // Our special version of (write) that generates less code due to not using parameters
    val lastValueWriter = datum"""(native-function system-library "llcore_write_stdout" (-> <any> <unit>))"""

    // Modify the last expression to print using lliby_write
    val wrappedDatum = ast.ProperList(List(
      lastValueWriter,
      program.last
    ))

    program.dropRight(1) :+ wrappedDatum
  }

  private def wrapForAssertRaises(errorPredicate : String, program : List[ast.Datum]) : List[ast.Datum] = {
    // Make sure we don't wrap any (import)s the test may have
    val (testImports, testExprs) = program.span {
      case ast.ProperList(ast.Symbol("import") :: _) => true
      case _ => false

    }

    (datum"(import (llambda error))" :: testImports) :+
      ast.ProperList(ast.Symbol("assert-raises") :: ast.Symbol(errorPredicate) :: testExprs)
  }

  private def utf8InputStreamToString(stream : InputStream) : String =
    Source.fromInputStream(stream, "UTF-8").mkString

  private def executeProgram(program : List[ast.Datum], schemeDialect : dialect.Dialect, optimizeLevel : Int) : ExecutionResult = {
    val finalProgram = testImportDecl :: program

    // Compile the program
    val outputFile = File.createTempFile("llambdafunc", null, null)
    outputFile.deleteOnExit()

    try {
      val compileConfig = CompileConfig(
        includePath=includePath,
        optimizeLevel=optimizeLevel,
        targetPlatform=targetPlatform,
        schemeDialect=schemeDialect
      )

      Compiler.compileData(finalProgram, outputFile, compileConfig)

      // Create our output logger
      var stdout : Option[InputStream] = None
      var stderr : Option[InputStream] = None

      val outputIO = new ProcessIO(
        stdin  => Unit, // Don't care
        stdoutStream => stdout = Some(stdoutStream),
        stderrStream => stderr = Some(stderrStream)
      )

      // Build our environment
      val testFilesBaseUrl = getClass.getClassLoader.getResource("test-files/")
      val testFilesBaseDir = new File(testFilesBaseUrl.toURI).getAbsolutePath

      val extraEnv = List[(String, String)](
        "LLAMBDA_TEST" -> "1",
        "LLAMBDA_TEST_FILES_BASE" -> testFilesBaseDir
      )

      // Call the program
      val testProcess = Process(
        command=outputFile.getAbsolutePath,
        cwd=None,
        extraEnv=extraEnv : _*
      ).run(outputIO)

      // Request the exit value now which will wait for the process to finish
      val exitValue = testProcess.exitValue()

      // Clean up the temporary executable
      outputFile.delete()

      val errorString = utf8InputStreamToString(stderr.get)

      if (AbnormalExitCodes.contains(exitValue)) {
        fail("Execution abnormally terminated with signal " + (exitValue - 128))
      }
      else if (exitValue == 0) {
        val outputString = utf8InputStreamToString(stdout.get)
        val output = SchemeParser.parseStringAsData(outputString)

        ExecutionResult(success=true, output=output, errorString=errorString)
      }
      else {
        ExecutionResult(success=false, output=Nil, errorString=errorString)
      }
    }
    finally {
      outputFile.delete()
    }
  }
}
