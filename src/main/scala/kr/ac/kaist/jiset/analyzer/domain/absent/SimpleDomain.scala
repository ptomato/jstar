package kr.ac.kaist.jiset.analyzer.domain.absent

import kr.ac.kaist.jiset.ir.state.Absent
import kr.ac.kaist.jiset.analyzer.domain._

object SimpleDomain extends generator.SimpleDomain(Absent) with absent.Domain
