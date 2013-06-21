package llambda.sst

import llambda._

sealed abstract class ScopedDatum {
  def unscope : ast.Datum
}

case class ScopedPair(car : ScopedDatum, cdr : ScopedDatum) extends ScopedDatum {
  def unscope = ast.Pair(car.unscope, cdr.unscope)
  override def toString = "(" + car + " . " + cdr + ")"
}

case class ScopedVectorLiteral(elements : Vector[ScopedDatum]) extends ScopedDatum {
  def unscope = ast.VectorLiteral(elements.map(_.unscope))
  override def toString = 
    "#(" + elements.map(_.toString).mkString(" ") + ")"
}

case class NonSymbolLeaf(atom : ast.NonSymbolLeaf) extends ScopedDatum {
  def unscope = atom
  override def toString = atom.toString
}

case class ScopedSymbol(scope : Scope, name : String) extends ScopedDatum {
  def unscope = ast.Symbol(name)
  override def toString = name + "@" + scope.hashCode.toHexString
}

// The following two objects are essential copied from AbstractSyntaxTree
// TODO: Find a way to share code that actually creates less code
object ScopedImproperList {
  def unapply(datum : ScopedDatum) : Option[(List[ScopedDatum], ScopedDatum)] = datum match {
    case ScopedPair(_, NonSymbolLeaf(ast.EmptyList)) => None // This is a proper list
    case ScopedPair(car, tail : ScopedPair)  => 
      ScopedImproperList.unapply(tail).map { case (head, terminator) =>
        (car :: head, terminator)
      }
    case ScopedPair(car, cdr) => Some(List(car), cdr)
    case _ => None
  }
}

object ScopedProperList {
  def unapply(datum : ScopedDatum) : Option[List[ScopedDatum]] = datum match {
    case NonSymbolLeaf(ast.EmptyList) => Some(Nil)
    case ScopedPair(car, cdr) => ScopedProperList.unapply(cdr).map(car :: _)
    case _ => None
  }
}

object ScopedDatum {
  def apply(scope : Scope, datum : ast.Datum) : ScopedDatum = datum match {
    case ast.Pair(car, cdr) => ScopedPair(ScopedDatum(scope, car), ScopedDatum(scope, cdr))
    case ast.VectorLiteral(elements) => ScopedVectorLiteral(elements.map(ScopedDatum.apply(scope, _)))
    case ast.Symbol(name) => ScopedSymbol(scope, name)
    case nonSymbol : ast.NonSymbolLeaf  => NonSymbolLeaf(nonSymbol)
  }
}