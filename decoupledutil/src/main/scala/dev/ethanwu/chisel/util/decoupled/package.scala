package nerv32

import chisel3._

package object util {
  def addOffset(lhs: UInt, rhs: SInt): UInt = {
    // XXX: consider supporting unknown widths?
    require(lhs.getWidth >= rhs.getWidth)

    (lhs.asSInt + rhs).asUInt
  }

  def anyMatch[T <: Data](choices: T*)(in: T): Bool =
    choices.map { in === _ }.reduce { _ || _ }
}
