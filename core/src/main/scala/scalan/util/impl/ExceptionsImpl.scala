package scalan.util

import scalan._
import scalan.common.Default
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait ExceptionsAbs extends scalan.Scalan with Exceptions {
  self: ExceptionsDsl =>

  // single proxy for each type family
  implicit def proxySThrowable(p: Rep[SThrowable]): SThrowable = {
    proxyOps[SThrowable](p)(scala.reflect.classTag[SThrowable])
  }

  // TypeWrapper proxy
  //implicit def proxyThrowable(p: Rep[Throwable]): SThrowable =
  //  proxyOps[SThrowable](p.asRep[SThrowable])

  implicit def unwrapValueOfSThrowable(w: Rep[SThrowable]): Rep[Throwable] = w.wrappedValue

  implicit lazy val throwableElement: Elem[Throwable] =
    element[SThrowable].asInstanceOf[WrapperElem[_, _]].baseElem.asInstanceOf[Elem[Throwable]]

  // familyElem
  class SThrowableElem[To <: SThrowable]
    extends WrapperElem[Throwable, To] {
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }
    override def isEntityType = true
    override lazy val tag = {
      weakTypeTag[SThrowable].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[SThrowable] => convertSThrowable(x) }
      tryConvert(element[SThrowable], this, x, conv)
    }

    def convertSThrowable(x: Rep[SThrowable]): Rep[To] = {
      x.selfType1 match {
        case _: SThrowableElem[_] => x.asRep[To]
        case e => !!!(s"Expected $x to have SThrowableElem[_], but got $e")
      }
    }
    lazy val baseElem = {
      new BaseTypeElem[Throwable, SThrowable](this.asInstanceOf[Elem[SThrowable]])(weakTypeTag[Throwable], DefaultOfThrowable)
    }
    lazy val eTo: Elem[_] = new SThrowableImplElem(isoSThrowableImpl)
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def sThrowableElement: Elem[SThrowable] =
    elemCache.getOrElseUpdate(
      (classOf[SThrowableElem[SThrowable]], Nil),
      new SThrowableElem[SThrowable]).asInstanceOf[Elem[SThrowable]]

  implicit case object SThrowableCompanionElem extends CompanionElem[SThrowableCompanionAbs] {
    lazy val tag = weakTypeTag[SThrowableCompanionAbs]
    protected def getDefaultRep = SThrowable
  }

  abstract class SThrowableCompanionAbs extends CompanionDef[SThrowableCompanionAbs] with SThrowableCompanion {
    def selfType = SThrowableCompanionElem
    override def toString = "SThrowable"
  }
  def SThrowable: Rep[SThrowableCompanionAbs]
  implicit def proxySThrowableCompanion(p: Rep[SThrowableCompanion]): SThrowableCompanion =
    proxyOps[SThrowableCompanion](p)

  // default wrapper implementation
  abstract class SThrowableImpl(val wrappedValue: Rep[Throwable]) extends SThrowable with Def[SThrowableImpl] {
    lazy val selfType = element[SThrowableImpl]

    def getMessage: Rep[String] =
      methodCallEx[String](self,
        this.getClass.getMethod("getMessage"),
        List())

    def initCause(cause: Rep[SThrowable]): Rep[SThrowable] =
      methodCallEx[SThrowable](self,
        this.getClass.getMethod("initCause", classOf[AnyRef]),
        List(cause.asInstanceOf[AnyRef]))
  }
  trait SThrowableImplCompanion
  // elem for concrete class
  class SThrowableImplElem(val iso: Iso[SThrowableImplData, SThrowableImpl])
    extends SThrowableElem[SThrowableImpl]
    with ConcreteElem[SThrowableImplData, SThrowableImpl] {
    override lazy val parent: Option[Elem[_]] = Some(sThrowableElement)
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }
    override lazy val eTo: Elem[_] = this
    override def convertSThrowable(x: Rep[SThrowable]) = // Converter is not generated by meta
!!!("Cannot convert from SThrowable to SThrowableImpl: missing fields List(wrappedValue)")
    override def getDefaultRep = SThrowableImpl(DefaultOfThrowable.value)
    override lazy val tag = {
      weakTypeTag[SThrowableImpl]
    }
  }

  // state representation type
  type SThrowableImplData = Throwable

  // 3) Iso for concrete class
  class SThrowableImplIso
    extends Iso[SThrowableImplData, SThrowableImpl] {
    override def from(p: Rep[SThrowableImpl]) =
      p.wrappedValue
    override def to(p: Rep[Throwable]) = {
      val wrappedValue = p
      SThrowableImpl(wrappedValue)
    }
    lazy val eTo = new SThrowableImplElem(this)
  }
  // 4) constructor and deconstructor
  class SThrowableImplCompanionAbs extends CompanionDef[SThrowableImplCompanionAbs] {
    def selfType = SThrowableImplCompanionElem
    override def toString = "SThrowableImpl"

    def apply(wrappedValue: Rep[Throwable]): Rep[SThrowableImpl] =
      mkSThrowableImpl(wrappedValue)
  }
  object SThrowableImplMatcher {
    def unapply(p: Rep[SThrowable]) = unmkSThrowableImpl(p)
  }
  lazy val SThrowableImpl: Rep[SThrowableImplCompanionAbs] = new SThrowableImplCompanionAbs
  implicit def proxySThrowableImplCompanion(p: Rep[SThrowableImplCompanionAbs]): SThrowableImplCompanionAbs = {
    proxyOps[SThrowableImplCompanionAbs](p)
  }

  implicit case object SThrowableImplCompanionElem extends CompanionElem[SThrowableImplCompanionAbs] {
    lazy val tag = weakTypeTag[SThrowableImplCompanionAbs]
    protected def getDefaultRep = SThrowableImpl
  }

  implicit def proxySThrowableImpl(p: Rep[SThrowableImpl]): SThrowableImpl =
    proxyOps[SThrowableImpl](p)

  implicit class ExtendedSThrowableImpl(p: Rep[SThrowableImpl]) {
    def toData: Rep[SThrowableImplData] = isoSThrowableImpl.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoSThrowableImpl: Iso[SThrowableImplData, SThrowableImpl] =
    cachedIso[SThrowableImplIso]()

  // 6) smart constructor and deconstructor
  def mkSThrowableImpl(wrappedValue: Rep[Throwable]): Rep[SThrowableImpl]
  def unmkSThrowableImpl(p: Rep[SThrowable]): Option[(Rep[Throwable])]

  registerModule(Exceptions_Module)
}

// Seq -----------------------------------
trait ExceptionsSeq extends scalan.ScalanSeq with ExceptionsDsl {
  self: ExceptionsDslSeq =>
  lazy val SThrowable: Rep[SThrowableCompanionAbs] = new SThrowableCompanionAbs {
    override def apply(msg: Rep[String]): Rep[SThrowable] =
      SThrowableImpl(new Throwable(msg))
  }

  // override proxy if we deal with TypeWrapper
  //override def proxyThrowable(p: Rep[Throwable]): SThrowable =
  //  proxyOpsEx[Throwable, SThrowable, SeqSThrowableImpl](p, bt => SeqSThrowableImpl(bt))

  case class SeqSThrowableImpl
      (override val wrappedValue: Rep[Throwable])
    extends SThrowableImpl(wrappedValue) with SeqSThrowable {
    override def getMessage: Rep[String] =
      wrappedValue.getMessage

    override def initCause(cause: Rep[SThrowable]): Rep[SThrowable] =
      SThrowableImpl(wrappedValue.initCause(cause))
  }

  def mkSThrowableImpl
    (wrappedValue: Rep[Throwable]): Rep[SThrowableImpl] =
    new SeqSThrowableImpl(wrappedValue)
  def unmkSThrowableImpl(p: Rep[SThrowable]) = p match {
    case p: SThrowableImpl @unchecked =>
      Some((p.wrappedValue))
    case _ => None
  }

  implicit def wrapThrowableToSThrowable(v: Throwable): SThrowable = SThrowableImpl(v)
}

// Exp -----------------------------------
trait ExceptionsExp extends scalan.ScalanExp with ExceptionsDsl {
  self: ExceptionsDslExp =>
  lazy val SThrowable: Rep[SThrowableCompanionAbs] = new SThrowableCompanionAbs {
    def apply(msg: Rep[String]): Rep[SThrowable] =
      newObjEx(classOf[SThrowable], List(msg.asRep[Any]))
  }

  case class ExpSThrowableImpl
      (override val wrappedValue: Rep[Throwable])
    extends SThrowableImpl(wrappedValue)

  object SThrowableImplMethods {
  }

  def mkSThrowableImpl
    (wrappedValue: Rep[Throwable]): Rep[SThrowableImpl] =
    new ExpSThrowableImpl(wrappedValue)
  def unmkSThrowableImpl(p: Rep[SThrowable]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: SThrowableImplElem @unchecked =>
      Some((p.asRep[SThrowableImpl].wrappedValue))
    case _ =>
      None
  }

  object SThrowableMethods {
    object getMessage {
      def unapply(d: Def[_]): Option[Rep[SThrowable]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SThrowableElem[_]] && method.getName == "getMessage" =>
          Some(receiver).asInstanceOf[Option[Rep[SThrowable]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[SThrowable]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object initCause {
      def unapply(d: Def[_]): Option[(Rep[SThrowable], Rep[SThrowable])] = d match {
        case MethodCall(receiver, method, Seq(cause, _*), _) if receiver.elem.isInstanceOf[SThrowableElem[_]] && method.getName == "initCause" =>
          Some((receiver, cause)).asInstanceOf[Option[(Rep[SThrowable], Rep[SThrowable])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[SThrowable], Rep[SThrowable])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SThrowableCompanionMethods {
    object apply {
      def unapply(d: Def[_]): Option[Rep[String]] = d match {
        case MethodCall(receiver, method, Seq(msg, _*), _) if receiver.elem == SThrowableCompanionElem && method.getName == "apply" =>
          Some(msg).asInstanceOf[Option[Rep[String]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[String]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object Exceptions_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVVPWwcRRR+t3Z8vh/iJDTYTYx14U/hzqRJ4SKyzheEdLGtrBXQJUKa2xufJ8zOjHfmnL0UKVJCh2gRSYnkjoqOBglRUCFAoqYKUERAKiLezO7e7VksScMWo52Zt+997/u+mT3+DU7pCF7RAeFENENqSNN375vaNPyOMMyMr8nBiNMtuv+S/OrBW5+vfOnBUg8WDoje0rwHleSlE6vJu08Pu1AhIqDayEgbeLnrKrQCyTkNDJOixcJwZEif01aXabPRhfm+HIwP4R6UunAmkCKIqKF+mxOtqU7XF6lFxCbzipuPd9S0hmjZLlq5LvYiwgzCxxpnkvjrVPljIcU4NHA6hbajLCyMKbNQychkJcqY7kAOsum8ILgA57q3yRFpYYlhyzcRE0P8sqZI8AEZ0m0MseHzCFhTvr83Vm4+14WqpodI0Duh4m4lVgCAClxyIJpTfpoTfpqWn4ZPI0Y4u0vs5m4k4zEkT2kOIFaY4uIzUmQZaEcMGh/eCm4+8WuhZz+OLZSy63ABE50vcIOTAnn85vrH+vHbDy97UO1BlenNvjYRCUxe8pStGhFCGod5QiCJhqjWWpFarsomxpywRCWQoSICM6VU1lEnzgJmbLBdq6fqFFBfNopmoaVYlSb9rhb063zTJpzvPlp+88Kvnfc88GZLVDClj8aPsqQGqv7eQSTvWNYdq3aopAQXl5o0/eqj3wdfr8Mtb0JVmvn51MEUp/RPP9S+f/2KB4s95+WrnAx7yJbucBruRG0pTA8W5RGNkp3yEeH27V/VKg/oPhlxk3KYb34OmzewWnjqFLXMbDiHlzICaolJt6Wgjau7jb/8bz85th6MoJ7sJMfwKbv898+n942zp4H6nYgoRQc3CB8lZ3/JwBye4pSVdKVSRH0qgB1WXPA5N8fDP1UrO5IruW+fSXp2tfzZW/f+WP7xMw8qyG2fmZCoxvpzHoj/0eQwS1DNRr7ruEwQLdjhfLZd7N0cgVbFaqKVL0N6du0xe//hR8YZthTP3ow7/dt4FW24j5ddlcYJRPVO3M5aXs9v2WH1P1A4qdAaL04xt/PcJTIqO549KasdL8wuoheqnTigzn14PdVSwUeGcQfoIra9VuACP9UAjXDvyafbb3z3xS/uqqhaNdHpwsz8PZx08QnvvjCtjv+HHFgD81bjCYjXCkEcWgvTEMs5f92HG8dblx6ETpklGic+upb7m8WOnvI/y5J9+gUIAAA="
}
}

trait ExceptionsDsl extends impl.ExceptionsAbs {self: ExceptionsDsl =>}
trait ExceptionsDslExp extends impl.ExceptionsExp {self: ExceptionsDslExp =>}
