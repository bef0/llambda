package llambda.codegen

import llambda.ProcedureSignature
import llambda.{celltype => ct}
import llambda.codegen.llvmir.{IrSignature, PointerType, VoidType, IntegerType}
import llambda.codegen.llvmir.IrFunction._

object ProcedureSignatureToIr {
  private def paramSignednessToAttribs(signedness : Option[Boolean]) : Set[ParameterAttribute] = {
    signedness match {
      case Some(true) => Set(SignExt)
      case Some(false) => Set(ZeroExt)
      case None => Set()
    }
  }

  def apply(signature : ProcedureSignature) : IrSignature = {
    val closureArgs = if (signature.hasClosureArg) {
      List(Argument(PointerType(IntegerType(8)), Set()))
    }
    else {
      Nil
    }

    val fixedArgs = signature.fixedArgs map (ValueTypeToIr(_)) map {
      case SignedFirstClassType(irType, signedness) =>
        Argument(irType, paramSignednessToAttribs(signedness))
    }

    val restArgs = if (signature.hasRestArg) {
      List(Argument(PointerType(ct.ListElementCell.irType), Set()))
    }
    else {
      Nil
    } 

    val allArgs = closureArgs ++ fixedArgs ++ restArgs

    val result = signature.returnType map (ValueTypeToIr(_)) match {
      case None => 
        Result(VoidType, Set())
      case Some(SignedFirstClassType(irType, signedness)) =>
        Result(irType, paramSignednessToAttribs(signedness))
    }

    IrSignature(result=result, arguments=allArgs)
  }
}