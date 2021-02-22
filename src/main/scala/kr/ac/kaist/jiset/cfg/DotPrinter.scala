package kr.ac.kaist.jiset.cfg

import kr.ac.kaist.jiset.ir._
import kr.ac.kaist.jiset.LINE_SEP
import kr.ac.kaist.jiset.util.Useful._

class DotPrinter {
  // for functions
  def apply(func: Function): DotPrinter = {
    val Function(_, entry, exit, nodes, edges) = func
    add(s"""digraph {""")
    nodes.foreach(apply)
    edges.foreach(apply)
    add(s"""}""")
  }

  // for nodes
  def apply(node: Node): DotPrinter = {
    val uid = node.uid
    node match {
      case Entry() =>
        add(s"""  node$uid [shape=point]""")
      case Exit() =>
        add(s"""  node$uid [shape=point]""")
      case Block(insts) =>
        add(s"""  node$uid [shape=none, margin=0, label=<""")
        add(s"""    <table border="0" cellborder="1" cellspacing="0" cellpadding="10">""")
        insts.foreach(inst => {
          add(s"""      <tr><td align="left">${norm(inst)}</td></tr>""")
        })
        add(s"""    </table>""")
        add(s"""  >]""")
      case Call(inst) =>
        add(s"""  node$uid [shape=cds, label=<${norm(inst)}>]""")
      case Branch(cond) =>
        add(s"""  node$uid [shape=diamond, label="${norm(cond)}"]""")
    }
  }

  // for edges
  def apply(edge: Edge): DotPrinter = edge match {
    case LinearEdge(from, next) =>
      add(s"""  node${from.uid} -> node${next.uid}""")
    case BranchEdge(from, thenNext, elseNext) =>
      add(s"""  node${from.uid} -> node${thenNext.uid} [label="true"]""")
      add(s"""  node${from.uid} -> node${elseNext.uid} [label="false"]""")
  }

  // string builder
  private val sb: StringBuilder = new StringBuilder

  // normalize beautified ir nodes
  private def norm(node: IRNode): String = escapeHtml(beautify(node, index = true))

  // add to StringBuilder
  private def add(str: String): DotPrinter = { sb.append(str + LINE_SEP); this }

  // conversion to string
  override def toString: String = sb.toString
}
