package kr.ac.kaist.jiset.analyzer.domain.cont

import kr.ac.kaist.jiset.ir.state.Cont
import kr.ac.kaist.jiset.analyzer.domain._

object SimpleDomain extends generator.SimpleDomain[Cont]
  with cont.Domain
