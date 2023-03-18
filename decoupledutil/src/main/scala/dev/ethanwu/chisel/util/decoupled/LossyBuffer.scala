package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, ReadyValidIO, Valid}

class LossyBufferIO[T <: Data](gen: T) extends Bundle {
  val in = Flipped(Valid(gen))
  val out = Decoupled(gen)
}

/** A one-element buffer that discards old values when a new value arrives
  *
  * @param gen
  *   the type of data to buffer
  * @param flow
  *   true if the data should be available on the same cycle (the input and
  *   output are combinatorially coupled)
  */
class LossyBuffer[T <: Data](gen: T, flow: Boolean = true) extends Module {
  val io = IO(new LossyBufferIO(gen))

  private val reg = Reg(gen)
  private val hasData = RegInit(false.B)

  io.out.valid := hasData
  io.out.bits := reg
  when(io.in.valid) {
    reg := io.in.bits
  }

  if (flow) {
    when(io.in.valid) {
      io.out.valid := true.B
      io.out.bits := io.in.bits
    }
    when(io.in.valid && !io.out.fire) {
      hasData := true.B
    }
    when(io.out.fire) {
      hasData := false.B
    }
  } else {
    when(io.out.fire) {
      hasData := false.B
    }
    when(io.in.valid) {
      hasData := true.B
    }
  }
}

object LossyBuffer {
  def apply[T <: Data](in: Valid[T], flow: Boolean): DecoupledIO[T] = {
    val buffer = Module(new LossyBuffer(chiselTypeOf(in.bits), flow))
    buffer.io.in := in
    buffer.io.out
  }

  def apply[T <: Data](in: ReadyValidIO[T], flow: Boolean): DecoupledIO[T] = {
    val buffer = Module(new LossyBuffer(chiselTypeOf(in.bits), flow))
    in.ready := true.B
    buffer.io.in.bits := in.bits
    buffer.io.in.valid := in.valid
    buffer.io.out
  }

  def apply[T <: Data](in: Valid[T]): DecoupledIO[T] = {
    apply(in, flow = true)
  }

  def apply[T <: Data](in: ReadyValidIO[T]): DecoupledIO[T] = {
    apply(in, flow = true)
  }
}
