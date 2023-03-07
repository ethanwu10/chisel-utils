package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.Decoupled

class SyncSRLatchIO extends Bundle {
  val set, reset = Flipped(Decoupled(Bits(0.W)))
  val out = Decoupled(Bool())
}

class SyncSRLatch(init: Bool = false.B, resetFirst: Boolean = false) extends Module {
  val io = IO(new SyncSRLatchIO())

  private val latch = SRLatch(init = init, resetFirst = resetFirst)

  io.out.bits := latch.out
  io.out.valid := true.B

  io.set.ready := io.out.ready
  io.reset.ready := io.out.ready
  latch.set := io.set.fire
  latch.reset := io.reset.fire
}

object SyncSRLatch {
  def apply(init: Bool = false.B, resetFirst: Boolean = false): SyncSRLatchIO = {
    val latch = Module(new SyncSRLatch(init = init, resetFirst = resetFirst))

    latch.io.set.noenq()
    latch.io.reset.noenq()

    latch.io
  }
}
