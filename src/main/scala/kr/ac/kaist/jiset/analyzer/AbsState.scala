package kr.ac.kaist.jiset.analyzer

import kr.ac.kaist.jiset.LINE_SEP
import kr.ac.kaist.jiset.ir._
import kr.ac.kaist.jiset.analyzer._
import kr.ac.kaist.jiset.util.{ Appender, StateMonad }
import kr.ac.kaist.jiset.util.Useful._

// abstract states
case class AbsState(
  reachable: Boolean,
  map: Map[String, AbsType] = Map()
) {
  import AbsState._

  // bottom check
  def isBottom: Boolean = !reachable

  // normalization
  def normalized: AbsState = {
    if (!reachable) Bot
    else if (map.exists { case (_, v) => v.isBottom }) Bot
    else this
  }

  // partial order
  def ⊑(that: AbsState): Boolean = (
    (!this.reachable || that.reachable) &&
    (this.map.keySet ++ that.map.keySet)
    .forall(key => this(key) ⊑ that(key))
  )

  // not partial order
  def !⊑(that: AbsState): Boolean = !(this ⊑ that)

  // join operator
  def ⊔(that: AbsState): AbsState = AbsState(
    reachable = this.reachable || that.reachable,
    map = {
      val keys = this.map.keySet ++ that.map.keySet
      val map = keys.map(key => key -> (this(key) ⊔ that(key))).toMap
      map.filter { case (_, v) => !v.isMustAbsent }
    }
  )

  // temporal variable check
  def isTemporalId(x: String): Boolean =
    x.startsWith("__") && x.endsWith("__")

  // define variable
  def define(x: String, t: AbsType, check: Boolean = false): AbsState = norm({
    if (t.isBottom) Bot
    else if (t.isMustAbsent) this
    else {
      if (check && exists(AbsId(x)) == AT && !isTemporalId(x))
        alarm(s"already defined variable: $x")
      copy(map = map + (x -> t))
    }
  })

  // lookup references
  def lookupVar(
    x: String,
    check: Boolean = true
  ): AbsType = norm {
    val AbsType(ts) = this(x)
    val local = ts - Absent
    val global = if (ts contains Absent) Global(x).set else Set()
    val t = AbsType(local ++ global)
    if (check && t.isMustAbsent && !isTemporalId(x)) {
      AbsSemantics.unknownVars += ((alarmCP, x))
      if (cfg.spec.grammar.nameMap.keySet contains x) AstT(x)
      else Absent
    } else {
      AbsSemantics.unknownVars -= ((alarmCP, x))
      t
    }
  }
  def lookupStrProp(
    base: Type,
    prop: String,
    check: Boolean = true
  ): AbsType = (base, prop) match {
    case (NormalT(t), _) =>
      val pureT = lookupStrProp(t, prop, false)
      if (pureT.isMustAbsent || pureT.isBottom) prop match {
        case "Type" => NORMAL.abs
        case "Value" => t.abs
        case "Target" => EMPTY.abs
        case _ => Absent.abs
      }
      else pureT
    case (AbruptT, "Type") => AbsType(BREAK, CONTINUE, RETURN, THROW)
    case (AbruptT, "Value") => AbsType(ESValueT, EMPTY)
    case (AbruptT, "Target") => AbsType(StrT)
    case _ => base.escaped.fold(AbsType.Bot)(_ match {
      case ESValueT => ESValueT.bases.foldLeft(AbsType.Bot) {
        case (t, base) => lookupStrProp(base, prop, check)
      }
      case (nameT: NameT) =>
        val t = nameT(prop)
        if (check && t.isMustAbsent) warning(s"unknown property: $base.$prop")
        t
      case NilT if prop == "length" => Num(0)
      case ListT(_) | StrT | Str(_) if prop == "length" => NumT
      case (record: RecordT) => record(prop)
      case MapT(elem) => elem
      case _ => AbsType.Bot
    })
  }
  def lookupGeneralProp(
    base: Type,
    prop: AbsType,
    check: Boolean = true
  ): AbsType = {
    var t = AbsType.Bot
    prop.set.foreach {
      case Str(prop) => t ⊔= lookupStrProp(base, prop, check)
      case _ =>
    }
    base.escaped.foreach(_ match {
      case MapT(elem) => t ⊔= elem
      case ListT(elem) if prop ⊑ NumT.abs => t ⊔= elem
      case StrT if prop ⊑ NumT.abs => t ⊔= StrT
      case _ =>
    })
    t
  }
  def lookup(
    ref: AbsRef,
    check: Boolean = true
  ): AbsType = norm(ref match {
    case AbsId(x) => lookupVar(x, check)
    case AbsStrProp(base, prop) =>
      base.set.map(lookupStrProp(_, prop, check)).foldLeft(AbsType.Bot)(_ ⊔ _)
    case AbsGeneralProp(base, prop) =>
      base.set.map(lookupGeneralProp(_, prop, check)).foldLeft(AbsType.Bot)(_ ⊔ _)
  })

  // existence check
  def exists(ref: AbsRef): AbsType = norm(ref match {
    case AbsGeneralProp(base, prop) => BoolT
    case _ => !lookup(ref, check = false).isAbsent
  })

  // update reference
  def update(ref: AbsRef, t: AbsType): AbsState = norm(ref match {
    case AbsId(x) => define(x, t)
    case AbsStrProp(_, _) if (lookup(ref) ⊓ t).isBottom =>
      warning(s"invalid property update: ${ref} with ${t}")
      this
    case _ => this
  })

  // contains operation
  def contains(list: AbsType, t: AbsType): AbsType = {
    notyet("contains")
    BoolT
  }

  // TODO not yet defined operations
  def append(t: AbsType, list: AbsType): AbsState = notyet("append")
  def prepend(t: AbsType, list: AbsType): AbsState = notyet("prepend")
  def delete(ref: AbsRef): AbsState = notyet("delete")
  def pop(list: AbsType, k: AbsType): (AbsType, AbsState) = notyet(Absent, "pop")

  // typeof operation
  def typeof(t: AbsType): AbsType = norm(AbsType(for {
    x <- t.set
    y <- x.typeNameSet
  } yield Str(y): Type))

  // is-instance-of operation
  def isInstanceOf(t: AbsType, name: String): AbsType = norm {
    val names = for {
      x <- t.set
      y <- x.instanceNameSet
    } yield y
    if (names.isEmpty) AbsType.Bot
    else if (names == Set(name)) AT
    else if (!names.contains(name)) AF
    else BoolT
  }
  def isInstanceOf(t: AbsType, name: String, k: Int): AbsType = BoolT

  // conversion to string
  override def toString: String = {
    val app = new Appender
    app.wrap(for ((x, t) <- map) {
      app :> x >> " -> " >> t.toString >> LINE_SEP
    })
    app.toString
  }

  // private helper functions
  private def norm(f: => AbsState): AbsState = if (isBottom) Bot else f
  private def norm(f: => AbsType): AbsType = if (isBottom) AbsType.Bot else f
  private def apply(x: String): AbsType =
    if (reachable) map.getOrElse(x, Absent.abs) else AbsType.Bot
  private def notyet(t: AbsType, name: String): (AbsType, AbsState) =
    (t, notyet(name))
  private def notyet(name: String) = norm {
    warning(s"not yet implemented: AbsState.$name")
    this
  }
}
object AbsState {
  // bottom value
  val Bot: AbsState = AbsState(reachable = false)

  // empty value
  val Empty: AbsState = AbsState(reachable = true)

  // monad helper
  val monad: StateMonad[AbsState] = new StateMonad[AbsState]
}
