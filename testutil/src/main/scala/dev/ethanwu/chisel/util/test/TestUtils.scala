package nerv32

import chisel3._
import chisel3.util._
import chiseltest._

object TestUtils {
  def monitor(runnable: => Unit) =
    fork.withRegion(Monitor)(runnable)

  implicit class DecoupledRecordDriver[T <: Record](x: ReadyValidIO[T])
      extends DecoupledDriver(x) {
    // Copied from chiseltest DecoupledDriver

    def expectPartialDequeue(data: T): T = {
      var value: Option[T] = None
      timescope {
        // TODO: check for init
        x.ready.poke(true.B)
        fork
          .withRegion(Monitor) {
            x.waitForValid()
            x.valid.expect(true.B)
            x.bits.expectPartial(data)
            value = Some(x.bits.peek())
          }
          .joinAndStep(x.getSinkClock)
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
          .joinAndStep(x.getSinkClock)
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
