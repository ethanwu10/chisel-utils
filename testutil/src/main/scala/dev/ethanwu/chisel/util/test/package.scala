package dev.ethanwu.chisel.util

import chisel3._
import chisel3.util._
import chiseltest._

package object test {
  def monitor(runnable: => Unit) =
    fork.withRegion(Monitor)(runnable)

  private class DecoupledDriverHelper[T <: Data](x: ReadyValidIO[T])
      extends DecoupledDriver(x) {
    // Make public
    override def getSourceClock: Clock = super.getSourceClock
    override def getSinkClock: Clock = super.getSinkClock
  }

  implicit class DecoupledDriverExt[T <: Data](x: ReadyValidIO[T]) {
    // Copied from [[chiseltest.DecoupledDriver]]

    private def getSinkClock: Clock = new DecoupledDriverHelper(x).getSinkClock

    def dequeue(): T = {
      var value: Option[T] = None
      // TODO: check for init
      x.ready.poke(true.B)
      fork
        .withRegion(Monitor) {
          x.waitForValid()
          x.valid.expect(true.B)
          value = Some(x.bits.peek())
        }
        .joinAndStep(getSinkClock)
      value.get
    }

    def dequeueNow(): T = {
      var value: Option[T] = None
      timescope {
        // TODO: check for init
        x.ready.poke(true.B)
        fork
          .withRegion(Monitor) {
            x.valid.expect(true.B)
            value = Some(x.bits.peek())
          }
          .joinAndStep(getSinkClock)
      }
      value.get
    }
  }

  implicit class DecoupledRecordDriver[T <: Record](x: ReadyValidIO[T])
      extends DecoupledDriver(x) {
    // Copied from [[chiseltest.DecoupledDriver]]

    def expectPartialDequeue(data: T): T = {
      var value: Option[T] = None
      timescope {
        // TODO: check for init
        x.ready.poke(true.B)
        fork
          .withRegion(Monitor) {
            waitForValid()
            x.valid.expect(true.B)
            x.bits.expectPartial(data)
            value = Some(x.bits.peek())
          }
          .joinAndStep(getSinkClock)
      }
      value.get
    }

    def expectPartialDequeueNow(data: T): T = {
      var value: Option[T] = None
      timescope {
        // TODO: check for init
        x.ready.poke(true.B)
        fork
          .withRegion(Monitor) {
            x.valid.expect(true.B)
            x.bits.expectPartial(data)
            value = Some(x.bits.peek())
          }
          .joinAndStep(getSinkClock)
      }
      value.get
    }

    def expectPartialDequeueSeq(data: Seq[T]): Unit = timescope {
      for (elt <- data) {
        expectPartialDequeue(elt)
      }
    }

    def expectPartialPeek(data: T): Unit = {
      fork.withRegion(Monitor) {
        x.valid.expect(true.B)
        x.bits.expectPartial(data)
      }
    }
  }
}
