package scalan
package compilation
package lms
package cxx
package sharedptr

import scala.virtualization.lms.common._
import scalan.compilation.lms.common.{CxxShptrGenJNIExtractor, JNILmsOpsExp}
import scalan.compilation.lms.CoreLmsBackendBase
import scalan.compilation.lms.common.CxxShptrGenLstOps

class CoreCxxShptrLmsBackend extends CoreLmsBackendBase with JNILmsOpsExp { self =>

  trait Codegen extends CxxShptrCodegen
  with CLikeGenEqual
  with CLikeGenPrimitiveOps
  with CxxShptrGenStruct
  with CxxShptrGenFatArrayLoopsFusionOpt
  with LoopFusionOpt
  with CxxShptrGenCastingOps
  with CxxShptrGenIfThenElseFat
  with CLikeGenOrderingOps
  with CLikeGenBooleanOps
  with CxxShptrGenFunctions
  with CxxShptrGenArrayOps
  with CxxShptrGenVariables
  with CxxShptrGenArrayBuilderOps
  with CxxShptrGenRangeOps
  with CLikeGenWhile
  with CLikeGenNumericOps
  with CxxShptrGenListOps
  with CxxShptrGenLstOps
  with CxxShptrGenJNIExtractor
  {
    override val IR: self.type = self

    override def shouldApplyFusion(currentScope: List[Stm])(result: List[Exp[Any]]): Boolean = true
//
//    override def hashCode(): Int = super.hashCode()
  }

  override val codegen = new Codegen {}
}
