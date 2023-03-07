package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.ReadyValidIO

class Splitter[T <: ReadyValidIO[Data]](gen: => T, n: Int) extends Module {
  val in = IO(Flipped(gen))
  val out = IO(Vec(n, gen))

  // Has this element been acked, now or in the past?
  private val eltAcks = Wire(Vec(n, Bool()))
  // Implementation detail for eltAcks - has this element been acked in the past?
  private val eltAcksReg = RegInit(VecInit.fill(n)(false.B))
  // Are all the downstream ports acked?
  private val allAcked = eltAcks.asUInt.andR

  for ((out, (ack, ackReg)) <- out zip (eltAcks zip eltAcksReg)) {
    out.bits := in.bits

    out.valid := Mux(ackReg, false.B, in.valid)

    ack := ackReg | out.fire
    ackReg := Mux(allAcked, false.B, ack)
  }

  in.ready := allAcked
}

object Splitter {
  def apply[T <: ReadyValidIO[Data]](n: Int, in: T): Vec[T] = {
    val splitter = Module(new Splitter(chiselTypeOf(in), n))
    splitter.in <> in
    splitter.out
  }
}
