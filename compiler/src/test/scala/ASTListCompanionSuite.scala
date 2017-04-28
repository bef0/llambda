package io.llambda.compiler

import org.scalatest.FunSuite


class ASTListCompanionSuite extends FunSuite {
  test("proper list creation") {
    assert(ast.ProperList(List()) === ast.EmptyList())

    assert(ast.ProperList(List(ast.Symbol("a"))) ===
      ast.Pair(ast.Symbol("a"), ast.EmptyList()))

    assert(ast.ProperList(List(ast.Symbol("a"), ast.Symbol("b"))) ===
      ast.Pair(ast.Symbol("a"),
        ast.Pair(ast.Symbol("b"),
          ast.EmptyList()
        )
      )
    )
  }

  test("any list creation") {
    assert(ast.AnyList(List(ast.Symbol("a")), ast.Symbol("b")) ===
      ast.Pair(ast.Symbol("a"), ast.Symbol("b"))
    )

    assert(ast.AnyList(List(ast.Symbol("a"), ast.Symbol("b")), ast.Symbol("c")) ===
      ast.Pair(ast.Symbol("a"),
        ast.Pair(ast.Symbol("b"), ast.Symbol("c")))
    )
  }

  test("proper list extraction") {
    assertResult(Some(Nil)) {
      ast.ProperList.unapply(ast.EmptyList())
    }

    assertResult(Some(List(ast.Symbol("a")))) {
      ast.ProperList.unapply(
        ast.Pair(ast.Symbol("a"), ast.EmptyList())
      )
    }

    assertResult(Some(List(ast.Symbol("a"), ast.Symbol("b")))) {
      ast.ProperList.unapply(
        ast.Pair(ast.Symbol("a"),
          ast.Pair(ast.Symbol("b"), ast.EmptyList())
        )
      )
    }

    assertResult(None) {
      ast.ProperList.unapply(ast.Symbol("a"))
    }

    assertResult(None) {
      ast.ProperList.unapply(
        ast.Pair(
          ast.Symbol("a"), ast.Symbol("b")
        )
      )
    }
  }

  test("improper list extraction") {
    assertResult(Some((List(ast.Symbol("a")), ast.Symbol("b")))) {
      ast.ImproperList.unapply(
        ast.Pair(
          ast.Symbol("a"), ast.Symbol("b")
        )
      )
    }

    assertResult(Some((List(ast.Symbol("a"), ast.Symbol("b")), ast.Symbol("c")))) {
      ast.ImproperList.unapply(
        ast.Pair(
          ast.Symbol("a"), ast.Pair(
            ast.Symbol("b"), ast.Symbol("c")
          )
        )
      )
    }

    assertResult(None) {
      ast.ImproperList.unapply(ast.Symbol("a"))
    }

    assertResult(None) {
      ast.ImproperList.unapply(
        ast.Pair(ast.Symbol("a"), ast.EmptyList())
      )
    }
  }

  test("any list extraction") {
    assertResult(Some((List(ast.Symbol("a")), ast.Symbol("b")))) {
      ast.AnyList.unapply(
        ast.Pair(
          ast.Symbol("a"), ast.Symbol("b")
        )
      )
    }

    assertResult(Some((List(ast.Symbol("a"), ast.Symbol("b")), ast.Symbol("c")))) {
      ast.AnyList.unapply(
        ast.Pair(
          ast.Symbol("a"), ast.Pair(
            ast.Symbol("b"), ast.Symbol("c")
          )
        )
      )
    }

    assertResult(None) {
      ast.AnyList.unapply(ast.Symbol("a"))
    }


    assertResult(Some((List(ast.Symbol("a")), ast.EmptyList()))) {
      ast.AnyList.unapply(
        ast.Pair(ast.Symbol("a"), ast.EmptyList())
      )
    }
  }
}

