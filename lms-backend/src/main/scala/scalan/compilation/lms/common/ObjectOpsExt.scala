package scalan.compilation.lms.common

import scala.reflect.SourceContext
import scala.virtualization.lms.common._

trait ObjectOpsExt extends Base {

  def newObj[A: Manifest](className: String, args: Seq[Rep[_]], newKeyWord: Boolean): Rep[A]
}

trait ObjectOpsExtExp extends ObjectOpsExt with BaseExp {

  case class NewObj[A: Manifest](className: String, args: Seq[Rep[_]], newKeyWord: Boolean) extends Def[A] {
    val m = manifest[A]
  }

  def newObj[A: Manifest](className: String, args: Seq[Rep[_]], newKeyWord: Boolean): Exp[A] = {
    NewObj[A](className, args, newKeyWord)
  }

  override def mirror[A: Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = e match {
    case NewObj(className, args, newKeyWord) => newObj(className, args.map(arg => f(arg)), newKeyWord)(mtype(manifest[A]))
    case _ => super.mirror(e, f)
  }
}

trait ScalaGenObjectOpsExt extends ScalaGenBase {
  val IR: ObjectOpsExtExp

  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case NewObj(className, args, newKeyWord) =>
      val newStr = newKeyWord match {
        case true => "new"
        case false => ""
      }
      emitValDef(sym, src"$newStr $className(${(args map quote).mkString(",")})")
    case _ => super.emitNode(sym, rhs)
  }
}
