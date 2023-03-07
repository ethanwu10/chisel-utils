package dev.ethanwu.chisel.util.decoupled

import chisel3._

object MakeBundle {
  implicit class BundleExt[T <: Record](x: T) {
    def makeUni(ops: (T => (Data, Data))*): T = {
      val wire = Wire(x.cloneType)
      for ((lhs, rhs) <- ops.map { _(wire) }) {
        lhs := rhs
      }
      wire
    }
    def makeBi(ops: (T => (Data, Data))*): T = {
      val wire = Wire(x.cloneType)
      for ((lhs, rhs) <- ops.map { _(wire) }) {
        lhs <> rhs
      }
      wire
    }
  }
}
