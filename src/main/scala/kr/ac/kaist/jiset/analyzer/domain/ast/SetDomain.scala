package kr.ac.kaist.jiset.analyzer.domain.ast

import kr.ac.kaist.jiset.ir.ASTVal
import kr.ac.kaist.jiset.analyzer.domain._

object SetDomain extends generator.SetDomain[ASTVal]
  with ast.Domain
