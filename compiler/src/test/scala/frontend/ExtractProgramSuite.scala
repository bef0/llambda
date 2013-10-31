package llambda.frontend

import org.scalatest.{FunSuite,Inside}
import llambda._

class ExtractProgramSuite extends FunSuite with Inside {
  def programFor(scheme : String) = {
    SchemeParser(scheme) match {
      case SchemeParser.Success(data, _) =>
        frontend.ExtractProgram(data)(new LibraryLoader, IncludePath())
      case err =>
        fail(err.toString)
    }
  }

  test("initial environment has no bindings") {
    intercept[UnboundVariableException] {
      programFor("(quote a)")
    }
  }
  
  test("import introduces bindings") {
    assert(programFor(
      """(import (llambda primitives)) 
         (quote a)"""
      ) === List(et.Literal(ast.Symbol("a"))))
  }
  
  test("multiple imports") {
    assert(programFor(
      """(import (only (llambda primitives) set!)) 
         (import (only (llambda primitives) lambda)) 
         set!
         lambda"""
      ) === List(et.VarRef(SchemePrimitives.Set), et.VarRef(SchemePrimitives.Lambda)))
  }

  test("program body is body context") {
    inside(programFor(
      """(import (llambda primitives))
         (define my-set set!)"""
    )) {
      case et.Bind((_, expression) :: Nil) :: Nil =>
        assert(expression === et.VarRef(SchemePrimitives.Set))
    }
  }
}
