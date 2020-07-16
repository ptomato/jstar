package kr.ac.kaist.jiset

import kr.ac.kaist.jiset.algorithm._
import kr.ac.kaist.jiset.spec.{ Grammar => SpecGrammar, Token => GrammarToken, _ }
import scala.util.parsing.combinator._
import scala.util.parsing.input._

object BugPatch extends RegexParsers {
  // es10-1
  val assertForAsyncIterator = true
  def patchAssertForAsyncIterator(algo: Algorithm): Unit = {
    algo.steps(6).tokens(2).asInstanceOf[StepList].steps(0).tokens =
      getTokens("Assert: id:iterationKind is const:iterate or const:async-iterate .")
  }

  // es10-2
  val ambiguousIfStatement = true
  def patchAmbiguousIfStatement(grammar: SpecGrammar): Unit = {
    val SpecGrammar(_, prods) = grammar
    prods.foreach {
      case Production(lhs @ Lhs("IfStatement", params), rhsList) =>
        rhsList(1).tokens :+= Lookahead(false, List(List(Terminal("else"))))
      case Production(lhs @ Lhs("CoverCallExpressionAndAsyncArrowHead", params), rhsList) =>
        lhs.params = List("Yield", "Await")
        rhsList(0).tokens(0).asInstanceOf[NonTerminal].args = List("?Yield", "?Await")
        rhsList(0).tokens(1).asInstanceOf[NonTerminal].args = List("?Yield", "?Await")
      case _ =>
    }
  }

  // es10-3
  val numberEqual = true
  def patchNumberEqual(algo: Algorithm): Unit = {
    algo.steps(6).tokens =
      getTokens("If id:index is value:-0 , return value:undefined .")
  }

  // es10-4
  val completionInAbstractEquality = true
  def patchCompletionInAbstractEquality(algo: Algorithm): Unit = {
    algo.steps(7).tokens =
      getTokens("If Type( id:x ) is either String, Number, or Symbol and Type( id:y ) is Object, return the result of the comparison id:x == ?ToPrimitive( id:y ) .")
    algo.steps(8).tokens =
      getTokens("If Type( id:x ) is Object and Type ( id:y ) is either String, Number, or Symbol, return the result of the comparison ?ToPrimitive( id:x ) == id:y .")
  }

  // es10-5
  val completionInEqualityExpr = true
  def patchCompletionInEqualityExpr(algo: Algorithm): Unit = {
    algo.steps :+= algo.steps.last.copy()
    algo.steps(5).tokens =
      getTokens("ReturnIfAbrupt( id:r ) .")
  }

  // es10-6
  val wrongArgsInPromiseResolve = true
  def patchWrongArgsInPromiseResolve(name: String, algo: Algorithm): Unit = name match {
    case "AsyncGeneratorResumeNext" =>
      algo.steps(9).tokens(8).asInstanceOf[StepList]
        .steps(1).tokens(6).asInstanceOf[StepList]
        .steps(0).tokens(12).asInstanceOf[StepList]
        .steps(1).tokens = getTokens("Let id:promise be ?PromiseResolve(%Promise%, id:completion .[[Value]]).")
    case "AsyncFromSyncIteratorContinuation" =>
      algo.steps(4).tokens =
        getTokens("Let id:valueWrapper be ?PromiseResolve(%Promise%, id:value ).")
    case "Await" =>
      algo.steps(1).tokens =
        getTokens("Let id:promise be ?PromiseResolve(%Promise%, id:value ).")
  }

  // es10-7
  val noIsFunctionDefinition = true

  // es10-8
  val noExpectedArgumentCount = true

  // es10-9
  val duplicatedVarScopedDecl = true
  def patchDuplicatedVarScopedDecl(algo: Algorithm): Unit = {
    algo.steps = List(
      Step(getTokens("Let id:declarations be a List containing nt:ForBinding .")),
      Step(getTokens("Append to id:declarations the elements of the VarScopedDeclarations of nt:Statement .")),
      Step(getTokens("Return id:declarations ."))
    )
  }

  private lazy val word = "[a-zA-Z0-9]+".r
  private lazy val symobol = ".".r
  private lazy val any = "\\S+".r
  private lazy val text = (word | symobol) ^^ { Text(_) }
  private lazy val id = "id:" ~> any ^^ { Id(_) }
  private lazy val value = "value:" ~> any ^^ { Value(_) }
  private lazy val const = "const:" ~> any ^^ { Const(_) }
  private lazy val nt = "nt:" ~> any ^^ { Nt(_) }
  private lazy val token = text ||| id ||| value ||| const ||| nt
  private lazy val tokens = rep1(token)
  def getTokens(str: String): List[Token] = parseAll(tokens, str).get
}
