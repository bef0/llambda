package io.llambda.llvmir

private[llvmir] object CallLikeInstructionBody {
  def apply(signature: IrSignatureLike, functionPtr: IrValue, arguments: Seq[IrValue]): String = {
    // Add our calling convention if we're using a non-default one
    val callingConvIrOpt = signature.callingConv.toOptIr

    // Only certain attributes are allowed on return values
    val filteredRetAttrs = signature.result.attributes.filter {
      case IrFunction.ZeroExt | IrFunction.SignExt | IrFunction.NonNull | IrFunction.Dereferenceable(_) => true
      case _ => false
    }

    val retAttrIrs = filteredRetAttrs.map(_.toIr)

    val resultTypeIr = signature.result.irType.toIr

    if (signature.hasVararg) {
      if (arguments.length < signature.arguments.length) {
        throw new InconsistentIrException(s"Passed ${arguments.length} arguments; expected at least ${signature.arguments.length}")
      }
    }
    else {
      if (arguments.length != signature.arguments.length) {
        throw new InconsistentIrException(s"Passed ${arguments.length} arguments; expected exactly ${signature.arguments.length}")
      }
    }

    // Make sure the argument types match the ones expected by the signature
    (arguments zip (signature.arguments)) map { case (arg, argDecl) =>
      if (arg.irType != argDecl.irType) {
        throw new InconsistentIrException(s"Argument passed with ${arg.irType}, signature as ${argDecl.irType}")
      }
    }

    val argIr = arguments.map(_.toIrWithType).mkString(", ")

    // Only noreturn, nounwind, readnone, and readonly are allowed here
    val filteredFuncAttrs = signature.attributes.intersect(Set(IrFunction.NoReturn, IrFunction.NoUnwind, IrFunction.ReadNone, IrFunction.ReadOnly))
    val funcAttrIrs = filteredFuncAttrs.map(_.toIr).toList.sorted

    // Start string building
    val callKernel = functionPtr.toIr + "(" + argIr + ")"

    val callBodyParts = callingConvIrOpt.toList ++ retAttrIrs ++ List(resultTypeIr) ++ List(callKernel) ++ funcAttrIrs

    callBodyParts.mkString(" ")
  }
}

