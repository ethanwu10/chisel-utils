package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.Valid

object ValidOut {
  def apply[D <: Data](enq: Valid[D]): Valid[D] = {
    val deq = Wire(Valid(chiselTypeOf(enq.bits)))
    deq.valid := enq.valid
    deq.bits := enq.bits
    deq
  }
}
