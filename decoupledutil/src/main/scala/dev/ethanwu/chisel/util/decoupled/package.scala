package dev.ethanwu.chisel.util

import chisel3._

/** Miscellaneous functions
  *
  * Many of these are not really related to Decoupled, but they might be useful
  * anyways.
  */
package object decoupled {

  /** Add a signed offset to an unsigned value
    *
    * Only supports both operands of known widths, and with an offset narrower
    * than the base.
    *
    * @param lhs
    *   unsigned base
    * @param rhs
    *   signed offset
    * @return
    *   the offset value
    */
  def addOffset(lhs: UInt, rhs: SInt): UInt = {
    // XXX: consider supporting unknown widths?
    require(lhs.getWidth >= rhs.getWidth)

    (lhs.asSInt + rhs).asUInt
  }

  /** Check if any of the `choices` match `in`
    *
    * @param choices
    *   values to check
    * @param in
    *   value to check for
    * @return
    *   do any of `choices` equal `in`?
    */
  def anyMatch[T <: Data](choices: T*)(in: T): Bool =
    choices.map { in === _ }.reduce { _ || _ }
}
