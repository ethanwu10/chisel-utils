package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, ReadyValidIO, Valid}

class LossyBufferIO[T <: Data](gen: T) extends Bundle {
  val in = Flipped(Valid(gen))
  val out = Decoupled(gen)
}

class LossyBuffer[T <: Data](gen: T) extends Module {
  val io = IO(new LossyBufferIO(gen))

  val reg = Reg(gen)
  val hasData = RegInit(false.B)

  io.out.valid := hasData || io.in.valid
  when(io.in.valid && !io.out.fire) {
    hasData := true.B
  }
  when(!io.in.valid && io.out.fire) {
    hasData := false.B
  }
  io.out.bits := reg
  when(io.in.valid) {
    reg := io.in.bits
    io.out.bits := io.in.bits
  }
}

object LossyBuffer {
  def apply[T <: Data](in: Valid[T]): DecoupledIO[T] = {
    val buffer = Module(new LossyBuffer(chiselTypeOf(in.bits)))
    buffer.io.in := in
    buffer.io.out
  }

  def apply[T <: Data](in: ReadyValidIO[T]): DecoupledIO[T] = {
    val buffer = Module(new LossyBuffer(chiselTypeOf(in.bits)))
    in.ready := true.B
    buffer.io.in.bits := in.bits
    buffer.io.in.valid := in.valid
    buffer.io.out
  }
}
