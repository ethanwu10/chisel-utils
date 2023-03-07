package dev.ethanwu.chisel.util.decoupled

import chisel3._

class SRLatchIO extends Bundle {
  val set, reset = Input(Bool())
  val out = Output(Bool())

  def doSet(): Unit = { set := true.B }
  def doReset(): Unit = { reset := true.B }
}

class SRLatch(init: Bool = false.B, resetFirst: Boolean = false)
    extends Module {
  val io = IO(new SRLatchIO())

  val reg = RegInit(init)
  io.out := reg

  def handleSet() = when(io.set) {
    reg := true.B
    io.out := true.B
  }
  def handleReset() = when(io.reset) {
    reg := false.B
    io.out := false.B
  }

  if (resetFirst) {
    handleReset()
    handleSet()
  } else {
    handleSet()
    handleReset()
  }
}

object SRLatch {
  def apply(init: Bool = false.B, resetFirst: Boolean = false): SRLatchIO = {
    val latch = Module(new SRLatch(init = init, resetFirst = resetFirst))

    latch.io.set := false.B
    latch.io.reset := false.B

    latch.io
  }
}
