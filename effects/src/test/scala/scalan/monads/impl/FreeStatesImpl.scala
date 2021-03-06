package scalan.monads

import scalan._
import scala.reflect.runtime.universe._
import scalan.collections.CollectionsDslExp
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait FreeStatesAbs extends scalan.ScalanDsl with FreeStates {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyStateF[S, A](p: Rep[StateF[S, A]]): StateF[S, A] = {
    proxyOps[StateF[S, A]](p)(scala.reflect.classTag[StateF[S, A]])
  }

  // familyElem
  class StateFElem[S, A, To <: StateF[S, A]](implicit _eS: Elem[S], _eA: Elem[A])
    extends EntityElem[To] {
    def eS = _eS
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateF[S, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[StateF[S, A]] => convertStateF(x) }
      tryConvert(element[StateF[S, A]], this, x, conv)
    }

    def convertStateF(x: Rep[StateF[S, A]]): Rep[To] = {
      x.selfType1 match {
        case _: StateFElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have StateFElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def stateFElement[S, A](implicit eS: Elem[S], eA: Elem[A]): Elem[StateF[S, A]] =
    cachedElem[StateFElem[S, A, StateF[S, A]]](eS, eA)

  implicit case object StateFCompanionElem extends CompanionElem[StateFCompanionAbs] {
    lazy val tag = weakTypeTag[StateFCompanionAbs]
    protected def getDefaultRep = StateF
  }

  abstract class StateFCompanionAbs extends CompanionDef[StateFCompanionAbs] with StateFCompanion {
    def selfType = StateFCompanionElem
    override def toString = "StateF"
  }
  def StateF: Rep[StateFCompanionAbs]
  implicit def proxyStateFCompanionAbs(p: Rep[StateFCompanionAbs]): StateFCompanionAbs =
    proxyOps[StateFCompanionAbs](p)

  abstract class AbsStateGet[S, A]
      (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends StateGet[S, A](f) with Def[StateGet[S, A]] {
    lazy val selfType = element[StateGet[S, A]]
  }
  // elem for concrete class
  class StateGetElem[S, A](val iso: Iso[StateGetData[S, A], StateGet[S, A]])(implicit override val eS: Elem[S], override val eA: Elem[A])
    extends StateFElem[S, A, StateGet[S, A]]
    with ConcreteElem[StateGetData[S, A], StateGet[S, A]] {
    override lazy val parent: Option[Elem[_]] = Some(stateFElement(element[S], element[A]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }

    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StateGet: missing fields List(f)")
    override def getDefaultRep = StateGet(constFun[S, A](element[A].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateGet[S, A]]
    }
  }

  // state representation type
  type StateGetData[S, A] = S => A

  // 3) Iso for concrete class
  class StateGetIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends EntityIso[StateGetData[S, A], StateGet[S, A]] with Def[StateGetIso[S, A]] {
    override def from(p: Rep[StateGet[S, A]]) =
      p.f
    override def to(p: Rep[S => A]) = {
      val f = p
      StateGet(f)
    }
    lazy val eFrom = element[S => A]
    lazy val eTo = new StateGetElem[S, A](self)
    lazy val selfType = new StateGetIsoElem[S, A](eS, eA)
    def productArity = 2
    def productElement(n: Int) = (eS, eA).productElement(n)
  }
  case class StateGetIsoElem[S, A](eS: Elem[S], eA: Elem[A]) extends Elem[StateGetIso[S, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new StateGetIso[S, A]()(eS, eA))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StateGetIso[S, A]]
    }
  }
  // 4) constructor and deconstructor
  class StateGetCompanionAbs extends CompanionDef[StateGetCompanionAbs] with StateGetCompanion {
    def selfType = StateGetCompanionElem
    override def toString = "StateGet"

    def apply[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
      mkStateGet(f)
  }
  object StateGetMatcher {
    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStateGet(p)
  }
  lazy val StateGet: Rep[StateGetCompanionAbs] = new StateGetCompanionAbs
  implicit def proxyStateGetCompanion(p: Rep[StateGetCompanionAbs]): StateGetCompanionAbs = {
    proxyOps[StateGetCompanionAbs](p)
  }

  implicit case object StateGetCompanionElem extends CompanionElem[StateGetCompanionAbs] {
    lazy val tag = weakTypeTag[StateGetCompanionAbs]
    protected def getDefaultRep = StateGet
  }

  implicit def proxyStateGet[S, A](p: Rep[StateGet[S, A]]): StateGet[S, A] =
    proxyOps[StateGet[S, A]](p)

  implicit class ExtendedStateGet[S, A](p: Rep[StateGet[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StateGetData[S, A]] = isoStateGet(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStateGet[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StateGetData[S, A], StateGet[S, A]] =
    reifyObject(new StateGetIso[S, A]()(eS, eA))

  // 6) smart constructor and deconstructor
  def mkStateGet[S, A](f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]]
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S => A])]

  abstract class AbsStatePut[S, A]
      (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends StatePut[S, A](s, a) with Def[StatePut[S, A]] {
    lazy val selfType = element[StatePut[S, A]]
  }
  // elem for concrete class
  class StatePutElem[S, A](val iso: Iso[StatePutData[S, A], StatePut[S, A]])(implicit override val eS: Elem[S], override val eA: Elem[A])
    extends StateFElem[S, A, StatePut[S, A]]
    with ConcreteElem[StatePutData[S, A], StatePut[S, A]] {
    override lazy val parent: Option[Elem[_]] = Some(stateFElement(element[S], element[A]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("S" -> Left(eS), "A" -> Left(eA))
    }

    override def convertStateF(x: Rep[StateF[S, A]]) = // Converter is not generated by meta
!!!("Cannot convert from StateF to StatePut: missing fields List(s, a)")
    override def getDefaultRep = StatePut(element[S].defaultRepValue, element[A].defaultRepValue)
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StatePut[S, A]]
    }
  }

  // state representation type
  type StatePutData[S, A] = (S, A)

  // 3) Iso for concrete class
  class StatePutIso[S, A](implicit eS: Elem[S], eA: Elem[A])
    extends EntityIso[StatePutData[S, A], StatePut[S, A]] with Def[StatePutIso[S, A]] {
    override def from(p: Rep[StatePut[S, A]]) =
      (p.s, p.a)
    override def to(p: Rep[(S, A)]) = {
      val Pair(s, a) = p
      StatePut(s, a)
    }
    lazy val eFrom = pairElement(element[S], element[A])
    lazy val eTo = new StatePutElem[S, A](self)
    lazy val selfType = new StatePutIsoElem[S, A](eS, eA)
    def productArity = 2
    def productElement(n: Int) = (eS, eA).productElement(n)
  }
  case class StatePutIsoElem[S, A](eS: Elem[S], eA: Elem[A]) extends Elem[StatePutIso[S, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new StatePutIso[S, A]()(eS, eA))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagA = eA.tag
      weakTypeTag[StatePutIso[S, A]]
    }
  }
  // 4) constructor and deconstructor
  class StatePutCompanionAbs extends CompanionDef[StatePutCompanionAbs] with StatePutCompanion {
    def selfType = StatePutCompanionElem
    override def toString = "StatePut"
    def apply[S, A](p: Rep[StatePutData[S, A]])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      isoStatePut(eS, eA).to(p)
    def apply[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
      mkStatePut(s, a)
  }
  object StatePutMatcher {
    def unapply[S, A](p: Rep[StateF[S, A]]) = unmkStatePut(p)
  }
  lazy val StatePut: Rep[StatePutCompanionAbs] = new StatePutCompanionAbs
  implicit def proxyStatePutCompanion(p: Rep[StatePutCompanionAbs]): StatePutCompanionAbs = {
    proxyOps[StatePutCompanionAbs](p)
  }

  implicit case object StatePutCompanionElem extends CompanionElem[StatePutCompanionAbs] {
    lazy val tag = weakTypeTag[StatePutCompanionAbs]
    protected def getDefaultRep = StatePut
  }

  implicit def proxyStatePut[S, A](p: Rep[StatePut[S, A]]): StatePut[S, A] =
    proxyOps[StatePut[S, A]](p)

  implicit class ExtendedStatePut[S, A](p: Rep[StatePut[S, A]])(implicit eS: Elem[S], eA: Elem[A]) {
    def toData: Rep[StatePutData[S, A]] = isoStatePut(eS, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoStatePut[S, A](implicit eS: Elem[S], eA: Elem[A]): Iso[StatePutData[S, A], StatePut[S, A]] =
    reifyObject(new StatePutIso[S, A]()(eS, eA))

  // 6) smart constructor and deconstructor
  def mkStatePut[S, A](s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]]
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]): Option[(Rep[S], Rep[A])]

  registerModule(FreeStates_Module)
}

// Seq -----------------------------------
trait FreeStatesSeq extends scalan.ScalanDslSeq with FreeStatesDsl {
  self: MonadsDslSeq =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs {
  }

  case class SeqStateGet[S, A]
      (override val f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStateGet[S, A](f) {
  }

  def mkStateGet[S, A]
    (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
    new SeqStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StateGet[S, A] @unchecked =>
      Some((p.f))
    case _ => None
  }

  case class SeqStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStatePut[S, A](s, a) {
  }

  def mkStatePut[S, A]
    (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
    new SeqStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p match {
    case p: StatePut[S, A] @unchecked =>
      Some((p.s, p.a))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreeStatesExp extends scalan.ScalanDslExp with FreeStatesDsl {
  self: MonadsDslExp =>
  lazy val StateF: Rep[StateFCompanionAbs] = new StateFCompanionAbs {
  }

  case class ExpStateGet[S, A]
      (override val f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStateGet[S, A](f)

  object StateGetMethods {
  }

  object StateGetCompanionMethods {
  }

  def mkStateGet[S, A]
    (f: Rep[S => A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StateGet[S, A]] =
    new ExpStateGet[S, A](f)
  def unmkStateGet[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StateGetElem[S, A] @unchecked =>
      Some((p.asRep[StateGet[S, A]].f))
    case _ =>
      None
  }

  case class ExpStatePut[S, A]
      (override val s: Rep[S], override val a: Rep[A])(implicit eS: Elem[S], eA: Elem[A])
    extends AbsStatePut[S, A](s, a)

  object StatePutMethods {
  }

  object StatePutCompanionMethods {
  }

  def mkStatePut[S, A]
    (s: Rep[S], a: Rep[A])(implicit eS: Elem[S], eA: Elem[A]): Rep[StatePut[S, A]] =
    new ExpStatePut[S, A](s, a)
  def unmkStatePut[S, A](p: Rep[StateF[S, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: StatePutElem[S, A] @unchecked =>
      Some((p.asRep[StatePut[S, A]].s, p.asRep[StatePut[S, A]].a))
    case _ =>
      None
  }

  object StateFMethods {
  }

  object StateFCompanionMethods {
    object unit {
      def unapply(d: Def[_]): Option[Rep[A] forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "unit" =>
          Some(a).asInstanceOf[Option[Rep[A] forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[A] forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object get {
      def unapply(d: Def[_]): Option[Unit forSome {type S}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem == StateFCompanionElem && method.getName == "get" =>
          Some(()).asInstanceOf[Option[Unit forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Unit forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object set {
      def unapply(d: Def[_]): Option[Rep[S] forSome {type S}] = d match {
        case MethodCall(receiver, method, Seq(s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "set" =>
          Some(s).asInstanceOf[Option[Rep[S] forSome {type S}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[S] forSome {type S}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object run {
      def unapply(d: Def[_]): Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}] = d match {
        case MethodCall(receiver, method, Seq(t, s, _*), _) if receiver.elem == StateFCompanionElem && method.getName == "run" =>
          Some((t, s)).asInstanceOf[Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeState[S, A]], Rep[S]) forSome {type S; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object FreeStates_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVWO4wbRRj+1z6/j1xCeCZK7jgZEBGxA02KKyJzOUdBzt3pNgUyEdF4PXY27M7s7YxPa4oUFCmgQzQUSESiQUqDqGgQDRKioIoQEhUFVQhCKUgF4p/Zhx/nNZxICrYY7cz+8z++7/tn9vY9yAkfXhAWcQiruVSSmqnfG0JWzQ0mbTm8xLsDh56nvWf415+88tmxLzOw1Ib8NSLOC6cNpfBlI/CSd5PutqBEmEWF5L6Q8FxLR6hb3HGoJW3O6rbrDiTpOLTesoVca8FCh3eHu3ADjBYctjizfCqpue4QIaiI1otUZWQn85KeD7e8UQxWV1XUx6q47BNbYvoY43Bov0M9c8g4G7oSDkWpbXkqLbQp2K7HfRmHKKC7a7wbTxcYwQV4vHWd7JE6hujXTenbrI87Kx6x3iZ9uokmynwBExbU6V0eenqebUFZ0F0E6KLrOXol8AAAGXhVJ1Eb4VNL8KkpfKom9W3i2O8Q9XHb58EQwsfIAgQeunj5H1zEHugG61bfu2K9+cCsuBm1OVCpFHSFeXS0nKIGTQXi+O3OB+L+hVtnM1BuQ9kWjY6QPrHkOOURWhXCGJc65wRA4veRrdU0tnSUBtpMSaJkcdcjDD1FUC4iT45t2VIZq7XFiJ0U6AvSo7GpEXhGUu9KSr1aN+vEcbbvPnv6+V833shAZjJECV2aKHw/diohb2K5tBk5V+OSBMMcIaymDT1VQykYjYU5uSSovHj3t+43Z+BKJsEyCv3v6EMXOfHjD5U7L53LQLGtxd50SL+NcIoNh7pb/jpnsg1Fvkf98EthjzjqbSadhS7tkYEjI5DH0ckiOhJWUtvSowq6Nd0CRgxAJVTxJme02tyu/mF+9+FtJVIfFsMvYZ/+ZZ/986dDPan1i4j2Ymyz2NsJFCfTmPVoc8CsOxc/Orp04urPmtd8l7vE1uI63oKcj52tCzkeQXsgGsthriZ36ZHV+/Zbt96XmjAjmDw6tjrXsVfX9L6Tc7iLj7DPb9588vdPrx7VrVfs2NIlXvXMARov7pNH2Fgwqf2lsCHWx4MUxtFS49PJqh6WkdIjet8FKid2Lo/tGYtzzIglpI0kZKgZJ7CgZD2zH8Ms0hw05jnYT72EYpyxdpGI8ES6CBG1p3ZaTzj3zn2VgdzrkOthmwlUX4cPWDemA+9CSQP5WrxmTNKB8BOfuAn8+lmBEVqTwm3ONGhMA1IxJiv+T+faPrpgCm1DzOjfdLL2bSdzts9L6pQeTz9UyW4P/m+SxYzHJZuukgPJaCzV/EyYs3igPTSRpVAzwfLU8gwGZzEfFje62h8ZVGp8d2QTGZabPqU6Mv41PRafJZyRroiK82E15Ygxo4Magb7x4OPNU99/8Yu+68rqyMcbliV/uaMDJZjqn9IlHQt/WscSRoWpS0An+zcgeoj9RAwAAA=="
}
}

