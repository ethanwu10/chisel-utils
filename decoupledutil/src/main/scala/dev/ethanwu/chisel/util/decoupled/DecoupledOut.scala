package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util._

object DecoupledOut {
  def apply[D <: Data](enq: IrrevocableIO[D]): IrrevocableIO[D] = {
    val deq = Wire(Irrevocable(chiselTypeOf(enq.bits)))
    deq.valid := enq.valid
    deq.bits := enq.bits
    enq.ready := deq.ready
    deq
  }
  def apply[D <: Data](enq: ReadyValidIO[D]): DecoupledIO[D] = {
    val deq = Wire(Decoupled(chiselTypeOf(enq.bits)))
    deq.valid := enq.valid
    deq.bits := enq.bits
    enq.ready := deq.ready
    deq
  }

  def apply[T <: Data](enq: IrrevocableIO[T], stall: Bool): IrrevocableIO[T] = {
    val deq = Wire(Irrevocable(chiselTypeOf(enq.bits)))
    deq.valid := enq.valid && !stall
    deq.bits := enq.bits
    enq.ready := deq.ready && !stall
    deq
  }
  def apply[T <: Data](enq: ReadyValidIO[T], stall: Bool): DecoupledIO[T] = {
    val deq = Wire(Decoupled(chiselTypeOf(enq.bits)))
    deq.valid := enq.valid && !stall
    deq.bits := enq.bits
    enq.ready := deq.ready && !stall
    deq
  }
}
