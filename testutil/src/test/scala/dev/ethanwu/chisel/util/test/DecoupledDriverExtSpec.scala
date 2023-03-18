package dev.ethanwu.chisel.util.test

import chisel3._
import chisel3.util.Queue
import chiseltest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.funspec.AnyFunSpec

class DecoupledDriverExtSpec extends AnyFunSpec with ChiselScalatestTester {
  def genHarness = new Queue(UInt(8.W), entries = 1)

  def initPorts(c: Queue[UInt]): Unit = {
    c.io.enq.initSource().setSourceClock(c.clock)
    c.io.deq.initSink().setSinkClock(c.clock)
  }

  describe("DecoupledRecordDriver") {
    it("doesn't break normal methods") {
      test(genHarness) { c =>
        initPorts(c)

        fork {
          c.io.enq.enqueue(0x12.U)
        }.fork {
          c.io.deq.expectDequeue(0x12.U)
        }.join()
      }
    }

    describe("dequeue") {
      it("can dequeue items") {
        test(genHarness) { c =>
          initPorts(c)

          c.io.enq.enqueue(0x12.U)

          val value = c.io.deq.dequeue()
          assert(value.litValue == 0x12)
        }
      }

      it("will wait for a value to become available") {
        test(genHarness) { c =>
          initPorts(c)

          fork {
            c.clock.step(10)
            c.io.enq.enqueue(0x12.U)
          }.fork {
            val value = c.io.deq.dequeue()
            assert(value.litValue == 0x12)
          }.join()
        }
      }
    }

    describe("dequeueNow") {
      it("can dequeue items") {
        test(genHarness) { c =>
          initPorts(c)

          c.io.enq.enqueue(0x12.U)

          val value = c.io.deq.dequeueNow()
          assert(value.litValue == 0x12)
        }
      }

      it("will fail if the value is not available") {
        assertThrows[TestFailedException] {
          test(genHarness) { c =>
            initPorts(c)

            fork {
              c.clock.step(10)
              c.io.enq.enqueue(0x12.U)
            }.fork {
              c.io.deq.dequeueNow()
            }.join()
          }
        }
      }
    }
  }
}
