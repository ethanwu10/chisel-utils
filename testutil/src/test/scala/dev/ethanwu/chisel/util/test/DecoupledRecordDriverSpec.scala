package dev.ethanwu.chisel.util.test

import chisel3._
import chisel3.util.Queue
import chisel3.experimental.BundleLiterals._
import chiseltest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.funspec.AnyFunSpec

class DecoupledRecordDriverSpec extends AnyFunSpec with ChiselScalatestTester {
  class HarnessBundle extends Bundle {
    val a = UInt(8.W)
    val b = Bool()
  }

  def genHarness = new Queue(new HarnessBundle, entries = 1)
  def initPorts(c: Queue[HarnessBundle]): Unit = {
    c.io.enq.initSource().setSourceClock(c.clock)
    c.io.deq.initSink().setSinkClock(c.clock)
  }

  describe("DecoupledRecordDriver") {
    it("doesn't break normal methods") {
      test(genHarness) { c =>
        initPorts(c)

        fork {
          c.io.enq.enqueue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
        }.fork {
          c.io.deq.expectDequeue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
        }.join()
      }
    }

    it("should support expectPartialDeq") {
      test(genHarness) { c =>
        initPorts(c)

        fork {
          c.io.enq.enqueue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
        }.fork {
          c.io.deq.expectPartialDequeue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
          ))
        }.join()
      }

      assertThrows[TestFailedException] {
        test(genHarness) { c =>
          initPorts(c)

          c.io.enq.enqueue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
          c.io.deq.expectPartialDequeue(new HarnessBundle().Lit(
            _.a -> 0x21.U,
          ))
        }
      }
    }

    it("should support expectPartialDeqNow") {
      test(genHarness) { c =>
        initPorts(c)

        c.io.enq.enqueue(new HarnessBundle().Lit(
          _.a -> 0x12.U,
          _.b -> true.B,
        ))
        c.io.deq.expectPartialDequeueNow(new HarnessBundle().Lit(
          _.a -> 0x12.U,
        ))
      }

      assertThrows[TestFailedException] {
        test(genHarness) { c =>
          initPorts(c)

          c.io.enq.enqueue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
          c.io.deq.expectPartialDequeueNow(new HarnessBundle().Lit(
            _.a -> 0x21.U,
          ))
        }
      }
    }

    it("should support expectPartialDeqSeq") {
      test(genHarness) { c =>
        initPorts(c)

        fork {
          c.io.enq.enqueueSeq(Seq(
            new HarnessBundle().Lit(
              _.a -> 0x12.U,
              _.b -> true.B,
            ),
            new HarnessBundle().Lit(
              _.a -> 0x13.U,
              _.b -> false.B,
            ),
            new HarnessBundle().Lit(
              _.a -> 0x14.U,
              _.b -> false.B,
            ),
          ))
        }.fork {
          c.io.deq.expectPartialDequeueSeq(Seq(
            new HarnessBundle().Lit(
              _.a -> 0x12.U,
            ),
            new HarnessBundle().Lit(
              _.a -> 0x13.U,
              _.b -> false.B,
            ),
            new HarnessBundle().Lit(
              _.b -> false.B,
            ),
          ))
        }.join()
      }

      assertThrows[TestFailedException] {
        test(genHarness) { c =>
          initPorts(c)

          fork {
            c.io.enq.enqueueSeq(Seq(
              new HarnessBundle().Lit(
                _.a -> 0x12.U,
                _.b -> true.B,
              ),
              new HarnessBundle().Lit(
                _.a -> 0x13.U,
                _.b -> false.B,
              ),
              new HarnessBundle().Lit(
                _.a -> 0x14.U,
                _.b -> false.B,
              ),
            ))
          }.fork {
            c.io.deq.expectPartialDequeueSeq(Seq(
              new HarnessBundle().Lit(
                _.a -> 0x12.U,
              ),
              new HarnessBundle().Lit(
                _.a -> 0x13.U,
                _.b -> false.B,
              ),
              new HarnessBundle().Lit(
                _.b -> true.B,
              ),
            ))
          }.join()
        }
      }
    }

    it("should support expectPartialPeek") {
      test(genHarness) { c =>
        initPorts(c)

        c.io.enq.enqueue(new HarnessBundle().Lit(
          _.a -> 0x12.U,
          _.b -> true.B,
        ))
        c.io.deq.expectPartialPeek(new HarnessBundle().Lit(
          _.a -> 0x12.U,
        ))
      }

      assertThrows[TestFailedException] {
        test(genHarness) { c =>
          initPorts(c)

          c.io.enq.enqueue(new HarnessBundle().Lit(
            _.a -> 0x12.U,
            _.b -> true.B,
          ))
          c.io.deq.expectPartialPeek(new HarnessBundle().Lit(
            _.a -> 0x21.U,
          ))
        }
      }
    }
  }
}
