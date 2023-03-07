package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.Decoupled

class SyncDFlopIO[T <: Data](gen: T) extends Bundle {
  val in = Flipped(Decoupled(gen))
  val out = Decoupled(gen)
}

class SyncDFlop[T <: Data](gen: T, init: T) extends Module {
  val io = IO(new SyncDFlopIO(gen))

  val reg = RegInit(gen, init)

  io.out.bits := reg
  io.out.valid := true.B

  io.in.ready := io.out.ready
  when(io.in.fire) {
    reg := io.in.bits
  }
}

object SyncDFlop {
  def apply[T <: Data](t: T, init: T): SyncDFlopIO[T] = {
    val flop = Module(new SyncDFlop(t, init))
    flop.io
  }
  def apply[T <: Data](init: T): SyncDFlopIO[T] = {
    SyncDFlop(chiselTypeOf(init), init)
  }
  def uninit[T <: Data](t: T): SyncDFlopIO[T] = {
    SyncDFlop(t, DontCare).asInstanceOf[SyncDFlopIO[T]]
  }
}
