package kr.ac.kaist.jiset.spec

import kr.ac.kaist.ires.ir
import kr.ac.kaist.jiset.util.Useful._
import kr.ac.kaist.jiset.{ ECMA262_DIR, SPEC_HTML, LINE_SEP }
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.mutable.Stack

// ECMASCript specifications
case class ECMAScript(grammar: Grammar, algos: List[Algo])

object ECMAScript {
  def apply(version: String, query: String, detail: Boolean): ECMAScript = {
    // read file content of spec.html
    val src = preprocess({
      val cur = currentVersion(ECMA262_DIR)
      println(s"Version: ${if (version == "") cur else version}")
      if (version == "") readFile(SPEC_HTML)
      else {
        changeVersion(version, ECMA262_DIR)
        val src = readFile(SPEC_HTML)
        changeVersion(cur, ECMA262_DIR)
        src
      }
    })
    // source lines
    implicit val lines = src.split(LINE_SEP)
    // parse html
    implicit val document = Jsoup.parse(src)
    // parse grammar
    implicit val grammar = parseGrammar
    // parse algorithm
    val algos =
      if (query == "") parseAlgo(document, detail)
      else getElems(document, query).toList.flatMap(parseAlgo(_, detail))
    // wrap grammar, algos
    ECMAScript(grammar, algos)
  }

  ////////////////////////////////////////////////////////////////////////////////
  // helper
  ////////////////////////////////////////////////////////////////////////////////
  // preprocess for spec.html
  def preprocess(src: String): String = {
    val lines = src.split(LINE_SEP)
    attachLines(dropAppendix(lines)).mkString(LINE_SEP)
  }

  // attach line numbers
  val startPattern = """(\s*<)([-a-z]+)([^<>]*(<[^<>]*>[^<>]*)*>[^<>]*)""".r
  val endPattern = """\s*</([-a-z]+)>\s*""".r
  val ignoreTags = Set("meta", "link", "style", "br", "img", "li", "p")
  def attachLines(lines: Array[String]): Array[String] = {
    val tagStack = Stack[(String, Int)]()
    var rngs = Map[Int, Int]()
    lines.zipWithIndex.foreach {
      case (line, k) => line match {
        case startPattern(_, tag, _, _) if !ignoreTags.contains(tag) =>
          tagStack.push((tag, k))
        case endPattern(tag) if !ignoreTags.contains(tag) =>
          val (expected, start) = tagStack.pop
          if (expected != tag)
            error(s"[ECMAScript.attachLines] $tag not matched with $expected")
          rngs += start -> (k + 1)
        case _ =>
      }
    }
    lines.zipWithIndex.map(_ match {
      case (line @ startPattern(pre, tag, post, _), start) if !ignoreTags.contains(tag) =>
        rngs.get(start).fold(line)(end => s"$pre$tag s=$start e=$end$post")
      case (line, _) => line
    })
  }

  // drop appendix lines
  def dropAppendix(lines: Array[String]): Array[String] = {
    val appendixLineNum = lines.indexWhere("<emu-annex.*annexB.*>".r matches _)
    if (appendixLineNum == -1) error("[ECMAScript.dropAppendix] not found Appendix.")
    lines.slice(0, appendixLineNum)
  }

  // check if inst has ??? or !!!
  def isComplete(inst: ir.Inst): Boolean = {
    var complete = true
    object Walker extends ir.UnitWalker {
      override def walk(expr: ir.Expr): Unit = expr match {
        case ir.ENotYetModeled(_) | ir.ENotSupported(_) => complete = false
        case _ => super.walk(expr)
      }
    }
    Walker.walk(inst)
    complete
  }

  ////////////////////////////////////////////////////////////////////////////////
  // grammar
  ////////////////////////////////////////////////////////////////////////////////
  // parse spec.html to Grammar
  def parseGrammar(
    implicit
    lines: Array[String],
    document: Document
  ): Grammar = {
    // get lexical grammar list
    val lexElem = document.getElementById("sec-lexical-grammar")
    val lexProdElems = lexElem
      .getElementsByTag("emu-prodref")
      .toArray(Array[Element]())
    val lexNames = lexProdElems.toList.map(_.attributes().get("name"))

    // codes for `emu-grammar` tagges elements
    val headPattern = "h\\d".r
    val prodElems = getElems(document, "emu-grammar").filter(elem => {
      val (found, pass) = toArray(elem.previousElementSiblings).foldLeft((false, true)) {
        case ((false, true), prev) => prev.tag.toString match {
          case headPattern() => (true, prev.text.endsWith("Syntax"))
          case _ => (false, true)
        }
        case (res, _) => res
      }
      found && pass
    })
    val prods = (for {
      prodElem <- prodElems
      body = getRawBody(prodElem).toList
      prodStr <- splitBy(body, "")
      prod <- optional(Production(prodStr))
    } yield prod).toList

    // partition prods
    val (lexProds, nonLexProds) =
      prods.partition(p => lexNames.contains(p.lhs.name))

    println(s"# of lexical production: ${lexProds.length}")
    println(s"# of non-lexical production: ${nonLexProds.length}")

    Grammar(lexProds, nonLexProds)
  }

  ////////////////////////////////////////////////////////////////////////////////
  // algorithm
  ////////////////////////////////////////////////////////////////////////////////
  // parse spec.html to Algo
  def parseAlgo(target: Element, detail: Boolean = false)(
    implicit
    lines: Array[String],
    grammar: Grammar
  ): List[Algo] = {
    // HTML elements with `emu-alg` tags
    val emuAlgs = getElems(target, "emu-alg")

    // HTML elements for Early Error
    val earlyErrors = for {
      parentElem <- getElems(target, "emu-clause[id$=early-errors]")
      elem <- getElems(parentElem, "ul")
    } yield elem

    val elems = emuAlgs ++ earlyErrors
    println(s"# algorithm elements: ${elems.size}")
    println(s"  - <emu-alg>: ${emuAlgs.size}")
    println(s"  - Early Error: ${earlyErrors.size}")

    // algorithms
    val (atime, algos) = time((for {
      elem <- elems
      algo <- Algo(elem, detail)
    } yield algo).toList)
    println(s"# algorithms: ${algos.length} ($atime ms)")

    // return algos
    algos
  }

  // pre-defined global identifiers
  val PREDEF = Set(
    // Completion-related ECMAScript internal algorithms
    "NormalCompletion", "ThrowCompletion", "ReturnIfAbrupt", "Completion",
    // ECMAScript type getter algorithm
    "Type",
    // JISET specific internal algorithms
    "IsAbruptCompletion", "WrapCompletion",
    // JISET specific global variables
    "GLOBAL_agent"
  )
}
