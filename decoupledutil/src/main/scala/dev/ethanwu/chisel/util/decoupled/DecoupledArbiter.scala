package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, PopCount, ReadyValidIO}

class DecoupledArbiter[T <: Data](gen: T, n: Int) extends Module {
  val in = IO(Vec(n, Flipped(Decoupled(gen))))
  val out = IO(Decoupled(gen))

  out.noenq()
  in.foreach { _.nodeq() }

  private val grant = WireDefault(VecInit.fill(n)(false.B))
  (0 until n) foreach { i =>
    when(grant.slice(0, i).reduceOption(_ | _).getOrElse(false.B)) {
      in(i).ready := true.B
    } otherwise {
      when(in(i).valid) {
        grant(i) := true.B
      }
    }
  }

  for ((in, grant) <- in zip grant) {
    when(grant) {
      out <> in
    }
  }

  when(grant.asUInt.orR) {
    assert(PopCount(grant) === 1.U, "grant should be one-hot")
  }
}

object DecoupledArbiter {
  def apply[T <: Data](ins: Seq[ReadyValidIO[T]]): DecoupledIO[T] = {
    val arbiter = Module(
      new DecoupledArbiter(chiselTypeOf(ins.head.bits), ins.length)
    )
    for ((a, b) <- arbiter.in zip ins) { a <> DecoupledOut(b) }
    arbiter.out
  }

  def input[T <: Data](out: ReadyValidIO[T], len: Int): Seq[DecoupledIO[T]] = {
    val arbiter = Module(new DecoupledArbiter(chiselTypeOf(out.bits), len))
    out <> arbiter.out
    arbiter.in
  }
}
