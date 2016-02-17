package framework

import org.scalajs.dom.Element
import rx._

import scala.util.{Failure, Success}
import scalatags.JsDom.all._

/**
 * A minimal binding between Scala.Rx and Scalatags and Scala-Js-Dom
 */
trait LowPriorityFramework { self: Framework =>

  /**
    * Wraps reactive strings in spans, so they can be referenced/replaced
    * when the Rx changes.
    */
  implicit def RxStr[T](r: Rx[T])(implicit f: T => Frag, ctx: Ctx.Owner): Frag = {
    RxHtmlTag(Rx(span(r())))
  }
}

trait Framework extends LowPriorityFramework {

  /**
    * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
    * to propagate changes into the DOM.
    */
  implicit def RxHtmlTag(n: Rx[HtmlTag])(implicit ctx: Ctx.Owner): Frag = {
    def rSafe = n match {
      case r: Rx.Dynamic[HtmlTag] => r.toTry match {
        case Success(v) => v.render
        case Failure(e) => span(e.getMessage, backgroundColor := "red").render
      }
      case v: Var[HtmlTag] => v.now.render
    }
    var last = rSafe
    n.triggerLater {
      val newLast = rSafe
      last.parentElement.replaceChild(newLast, last)
      last = newLast
    }
    bindNode(last)
  }

  implicit def RxAttrValue[T](implicit ctx: Ctx.Owner, av: AttrValue[T]) = new AttrValue[Rx[T]]{
    def apply(t: Element, a: Attr, r: Rx[T]): Unit = {
      r.trigger { av.apply(t, a, r.now)}
    }
  }

  implicit def RxStyleValue[T](implicit ctx: Ctx.Owner, sv: StyleValue[T]) = new StyleValue[Rx[T]]{
    def apply(t: Element, s: Style, r: Rx[T]): Unit = {
      r.trigger { sv.apply(t, s, r.now) }
    }
  }
}

object Framework extends Framework
