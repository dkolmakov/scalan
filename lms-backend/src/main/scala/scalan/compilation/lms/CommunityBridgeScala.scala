package scalan.compilation.lms

import java.lang.reflect.Method

import scalan.collections.SeqsScalaMethodMapping
import scalan.collections.impl.CollectionsExp
import scalan.compilation.language.ScalaInterpreter

trait CommunityBridgeScala extends CommunityBridge with SeqsScalaMethodMapping with ScalaInterpreter {
  import scalan._

  override def transformMethodCall[T](m: LmsMirror, receiver: Exp[_], method: Method, args: List[AnyRef], returnType: Elem[T]): lms.Exp[_] = {
    import lms.EffectId._

    mappedFunc(method) match {
      case Some(func: ScalaMappingDSL#ScalaFunc) => func.lib match {
        case e: ScalaMappingDSL#ScalaLib =>
          val param = func.wrapper match {
            case true => Seq(m.symMirrorUntyped(receiver))
            case false => Seq.empty[lms.Exp[_]]
          }
          val methodName:String = func.name match {
            case n: String if n.isEmpty => n
            case _ =>
              e.pack match {
                case p if p.isEmpty => func.name
                case p => p + "." + func.name
              }
          }
          elemToManifest(returnType) match {
            case (mA: Manifest[a]) =>
              val lmsArgs = param ++ args.collect { case v: Exp[_] => m.symMirrorUntyped(v) }
              lms.scalaMethod[a](null, PURE, methodName, List.empty, lmsArgs: _*)(mA.asInstanceOf[Manifest[a]])
          }
        case e: ScalaMappingDSL#EmbeddedObject if e.name == "lms" =>
          val obj = m.symMirrorUntyped(receiver)
          val name = func.name
          import scala.reflect.runtime.universe._
          val instanceMirror = runtimeMirror(obj.getClass.getClassLoader).reflect(lms)
          val lmsMethod = instanceMirror.symbol.typeSignature.member(TermName(name))
          instanceMirror.reflectMethod(lmsMethod.asMethod).apply(obj, elemToManifest(receiver.elem)).asInstanceOf[lms.Exp[_]]
      }
      case Some(nonScalaFunc) =>
        !!!(s"$nonScalaFunc is not a ScalaMappingDSL#ScalaFunc")
      case None =>
        val obj = m.symMirrorUntyped(receiver)
        elemToManifest(returnType) match {
          case (mA: Manifest[a]) => lms.scalaMethod[a](obj, PURE, method.getName,
            args.collect {
              case el: WrapperElem1[_, _, _, _] => el.baseElem.tag
              case elem: Element[_] => elem.tag
            },
            /* filter out implicit ClassTag params */
            args.collect { case v: Exp[_] => m.symMirrorUntyped(v) }: _*)(mA.asInstanceOf[Manifest[a]])
        }
    }
  }

  // Removing causes MethodCallItTests.Class Mapping to fail, error is that Scala field arr is not represented
  // as a Java field (because ExpCollectionOverArray inherits it from CollectionOverArray).
  // TODO implement this case generically
  override protected def transformDef[T](m: LmsMirror, g: AstGraph, sym: Exp[T], d: Def[T]) = d match {
    case u: CollectionsExp#ExpCollectionOverArray[_] =>
      val exp = Manifest.classType(u.getClass) match {
        case (mA: Manifest[a]) =>
          lms.newObj[a]("scalan.imp.ArrayImp", Seq(m.symMirrorUntyped(u.arr.asInstanceOf[Exp[_]])), true)(mA)
      }
      m.addSym(sym, exp)
    case _ => super.transformDef(m, g, sym, d)
  }
}
