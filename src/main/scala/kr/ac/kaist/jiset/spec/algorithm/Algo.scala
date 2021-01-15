package kr.ac.kaist.jiset.spec.algorithm

import kr.ac.kaist.ires.ir
import kr.ac.kaist.jiset.LINE_SEP
import kr.ac.kaist.jiset.algorithm.GeneralAlgoCompiler
import kr.ac.kaist.jiset.spec.ECMAScript
import kr.ac.kaist.jiset.spec.grammar._
import kr.ac.kaist.jiset.util.Useful._
import org.jsoup.nodes._
import scala.util.matching.Regex._

// ECMASCript abstract algorithms
case class Algo(head: AlgoHead, body: ir.Inst) {
  // head fields
  def name: String = head.name
  def params: List[String] = head.params

  // completion check (not containing ??? or !!! in the algorithm body)
  def isComplete: Boolean = {
    var complete = true
    object Walker extends ir.UnitWalker {
      override def walk(expr: ir.Expr): Unit = expr match {
        case ir.ENotYetModeled(_) | ir.ENotSupported(_) => complete = false
        case _ => super.walk(expr)
      }
    }
    Walker.walk(body)
    complete
  }

  // conversion to string
  override def toString: String =
    s"$head ${ir.beautify(body)}"
}
object Algo {
  // get algorithms
  def parse(
    elem: Element,
    detail: Boolean = false
  )(
    implicit
    lines: Array[String],
    grammar: Grammar
  ): List[Algo] = {
    if (detail) println(s"--------------------------------------------------")
    val result = try {
      val heads = getHeads(elem)
      if (detail) {
        heads.foreach {
          case AlgoHead(name, params) => println(s"$name (${params.mkString(", ")}):")
        }
      }
      val code = getRawBody(elem)
      if (detail) {
        code.foreach(println _)
        println(s"====>")
      }
      val body = getBody(code)
      if (detail) println(ir.beautify(body))
      heads.map(Algo(_, body))
    } catch {
      case e: Throwable =>
        if (detail) println(s"[Algo] ${e.getMessage}")
        Nil
    }
    if (detail) println(s"--------------------------------------------------")
    result
  }

  // get names and parameters
  val paramPattern = "[^\\s,()\\[\\]]+".r
  val namePattern = "[.:a-zA-Z0-9%\\[\\]@ /`_-]+".r
  val prefixPattern = ".*Semantics:".r
  val withParamPattern = "_\\w+_".r
  def nameCheck(name: String): Boolean =
    namePattern.matches(name) && !ECMAScript.PREDEF.contains(name)
  def getHeads(elem: Element)(
    implicit
    lines: Array[String],
    grammar: Grammar
  ): List[AlgoHead] = {
    def rename(params: List[String]): List[String] = {
      val duplicated = params.filter(p => params.count(_ == p) > 1).toSet.toList
      var counter: Map[String, Int] = Map()
      params.map(p => {
        if (duplicated contains p) {
          val n = counter.getOrElse(p, 0)
          counter = counter + (p -> (n + 1))
          p + n.toString
        } else p
      })
    }

    def trimParam(m: Match): String = {
      val s = m.toString
      s.substring(1, s.length - 1)
    }

    val headElem = elem.siblingElements.get(0)
    if (headElem.tag.toString != "h1") error(s"no algorithm head: $headElem")
    val str = headElem.text

    // extract name
    val from = str.indexOf("(")
    var name = if (from == -1) str else str.substring(0, from)
    name = prefixPattern.replaceFirstIn(name, "").trim
    if (!nameCheck(name)) error(s"not target algorithm: $str")

    // extract parameters
    val params = if (from == -1) Nil else paramPattern
      .findAllMatchIn(str.substring(from))
      .map(trimParam(_)).toList

    val prev = elem.previousElementSibling
    if (prev.tag.toString == "emu-grammar") {
      // syntax-directed algorithms
      val idxMap = grammar.idxMap

      // with parameters
      val withParams: List[String] =
        toArray(elem.previousElementSiblings).toList.flatMap(prevElem => {
          val isParagraph = prevElem.tag.toString == "p"
          val text = prevElem.text
          val isParams = text.startsWith("With parameter")
          if (isParagraph && isParams)
            withParamPattern.findAllMatchIn(text).map(trimParam(_)).toList
          else List.empty
        })

      val body = getRawBody(prev).toList
      for {
        code <- splitBy(body, "")
        prod = Production(code)
        lhsName = prod.lhs.name
        rhs <- prod.rhsList
        rhsName <- rhs.names
        syntax = lhsName + ":" + rhsName
        (i, j) <- idxMap.get(syntax)
        newName = s"$lhsName[$i,$j].$name"
        // numbering duplicated params
        ntParams = rename(rhs.getNTs.map(_.name))
        newParams = withParams ++ ntParams ++ params
      } yield AlgoHead(newName, newParams)
    } else if (false) {
      // TODO built-in algorithms - handle parameters
      ???
    } else {
      // normal algorithms
      List(AlgoHead(name, params))
    }
  }

  // get body instructions
  def getBody(code: Iterable[String])(implicit grammar: Grammar): ir.Inst = {
    val tokens = (new Tokenizer).getTokens(code)
    GeneralAlgoCompiler.compile(tokens)
  }
}

case class AlgoHead(name: String, params: List[String]) {
  override def toString: String = s"$name (${params.mkString(", ")})"
}
