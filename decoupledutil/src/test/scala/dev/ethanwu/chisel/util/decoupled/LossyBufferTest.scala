package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chiseltest._
import dev.ethanwu.chisel.util.test._
import org.scalatest.funspec.AnyFunSpec

class LossyBufferTest extends AnyFunSpec with ChiselScalatestTester {
  describe("LossyBuffer") {
    def buffer(flow: Boolean) = new LossyBuffer(UInt(8.W), flow)

    def initPorts(c: LossyBuffer[UInt]) = {
      c.io.in.initSource().setSourceClock(c.clock)
      c.io.out.initSink().setSinkClock(c.clock)
    }

    def testSequence(mod: => LossyBuffer[UInt], enqDelays: Seq[Int], deqDelays: Seq[Int]) = {
      test(mod).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        fork {
          enqDelays.zipWithIndex.foreach { case (delay, i) =>
            c.clock.step((delay - 1) max 0)
            c.io.in.enqueueNow(i.U)
          }
        }.fork {
          var currIdx = BigInt(-1)
          deqDelays.foreach { delay =>
            c.clock.step((delay - 1) max 0)
            val nextIdx = c.io.out.dequeue().litValue
            assert(nextIdx > currIdx)

            currIdx = nextIdx
          }
        }.join()
      }
    }

    def commonTests(bufferGen: => LossyBuffer[UInt]): Unit = {
      it("latches data for many cycles") {
        test(buffer(false)) { c =>
          initPorts(c)

          c.io.in.enqueueNow(0x12.U)
          c.clock.step(5)
          c.io.out.expectDequeueNow(0x12.U)
        }
      }

      it("handles enqueuing and dequeuing as fast as possible") {
        testSequence(bufferGen, Seq(1, 1, 1, 1), Seq(1, 1, 1, 1))
      }

      it("handles dequeuing as fast as possible with slower enqueue") {
        testSequence(bufferGen, Seq(1, 2, 1, 10), Seq(1, 1, 1, 1))
      }

      it("handles dequeuing slower than enqueuing") {
        testSequence(bufferGen, Seq.fill(10) { 1 }, Seq(2, 3, 1, 4))
      }

      it("handles dequeuing when full on same cycle as enqueue") {
        testSequence(bufferGen, Seq(1, 1, 2, 2, 1), Seq(2, 1, 3))
      }
    }

    describe("without flow") {
      def gen = buffer(flow = false)

      commonTests(gen)
    }

    describe("with flow") {
      def gen = buffer(flow = true)

      commonTests(gen)

      it("flows data through") {
        test(gen) { c =>
          initPorts(c)

          fork {
            c.io.in.enqueueNow(0x12.U)
          }.fork {
            c.io.out.expectDequeueNow(0x12.U)
          }.join()

          c.io.in.enqueueNow(0x14.U)
          c.io.out.expectDequeueNow(0x14.U)
        }
      }
    }
  }
}
