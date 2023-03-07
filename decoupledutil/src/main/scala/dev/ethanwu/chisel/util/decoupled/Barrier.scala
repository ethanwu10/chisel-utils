package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, ReadyValidIO}

/** Synchronization barrier for a [[Bundle]] of [[Decoupled]]s
  *
  * The output (downstream) bundle elements are made valid only when all of the
  * input (upstream) bundle elements are valid, ane likewise the transaction on
  * the upstream bundle elements only fires once the transaction has been
  * acknowledged on all downstream elements.
  *
  * @param gen
  *   The bundle - all elements of the bundle must be [[Decoupled]] (and not
  *   flipped)
  */
class Barrier[T <: Aggregate](gen: T) extends Module {
  val in = IO(Flipped(gen))
  val out = IO(gen)

  private val numElts = in.getElements.length

  // Upstream valids
  private val valids = Wire(Vec(numElts, Bool()))
  // Are all upstreams valid? If so, send downstream
  private val allValid = valids.asUInt.andR
  // Has this element been acked, now or in the past?
  private val eltAcks = Wire(Vec(numElts, Bool()))
  // Implementation detail for eltAcks - has this element been acked in the past?
  private val eltAcksReg = RegInit(VecInit.fill(numElts)(false.B))
  // Are all the downstream ports acked?
  private val allAcked = eltAcks.asUInt.andR

  for (
    (((inRaw, outRaw), valid), (ack, ackReg)) <-
      ((in.getElements zip out.getElements) zip valids) zip
        (eltAcks zip eltAcksReg)
  ) {
    val in = inRaw.asInstanceOf[ReadyValidIO[Data]]
    val out = outRaw.asInstanceOf[ReadyValidIO[Data]]

    out.bits := in.bits

    valid := in.valid
    out.valid := Mux(ackReg, false.B, allValid)

    ack := ackReg | out.fire
    ackReg := Mux(allAcked, false.B, ack)

    in.ready := allAcked
  }
}

object Barrier {
  def apply[T <: Aggregate](in: T): T = {
    val barrier = Module(new Barrier(chiselTypeOf(in)))
    barrier.in <> in
    barrier.out
  }

  def group[T1 <: Data, T2 <: Data](
      in1: ReadyValidIO[T1],
      in2: ReadyValidIO[T2]
  ): (DecoupledIO[T1], DecoupledIO[T2]) = {
    val barrier = Module(new Barrier(new Bundle {
      val v1 = Decoupled(chiselTypeOf(in1.bits))
      val v2 = Decoupled(chiselTypeOf(in2.bits))
    }))
    barrier.in.v1 <> DecoupledOut(in1)
    barrier.in.v2 <> DecoupledOut(in2)
    (barrier.out.v1, barrier.out.v2)
  }

  def group[T1 <: Data, T2 <: Data, T3 <: Data](
      in1: ReadyValidIO[T1],
      in2: ReadyValidIO[T2],
      in3: ReadyValidIO[T3]
  ): (DecoupledIO[T1], DecoupledIO[T2], DecoupledIO[T3]) = {
    val barrier = Module(new Barrier(new Bundle {
      val v1 = Decoupled(chiselTypeOf(in1.bits))
      val v2 = Decoupled(chiselTypeOf(in2.bits))
      val v3 = Decoupled(chiselTypeOf(in3.bits))
    }))
    barrier.in.v1 <> DecoupledOut(in1)
    barrier.in.v2 <> DecoupledOut(in2)
    barrier.in.v3 <> DecoupledOut(in3)
    (barrier.out.v1, barrier.out.v2, barrier.out.v3)
  }
}
