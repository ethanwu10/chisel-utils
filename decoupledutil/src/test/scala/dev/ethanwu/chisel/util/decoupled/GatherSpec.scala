package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import chiseltest._
import nerv32.TestUtils._
import dev.ethanwu.chisel.util.decoupled.MakeBundle._
import org.scalatest.funspec.AnyFunSpec

class GatherSpec extends AnyFunSpec with ChiselScalatestTester {
  class HarnessBundleIn extends Bundle {
    val a = Decoupled(UInt(8.W))
    val b = Decoupled(SInt(4.W))
  }
  class HarnessBundleOut extends Bundle {
    val a = UInt(8.W)
    val b = SInt(4.W)
  }
  describe("Gather") {
    def gather = new Gather(new HarnessBundleIn, new HarnessBundleOut)

    def initPorts(c: Gather[HarnessBundleIn, HarnessBundleOut]) = {
      c.in.a.initSource().setSourceClock(c.clock)
      c.in.b.initSource().setSourceClock(c.clock)
      c.out.initSink().setSinkClock(c.clock)
    }

    it("transfers all on the same cycle") {
      test(gather).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        fork {
          c.in.a.enqueueNow(0xa1.U)
        }.fork {
          c.in.b.enqueueNow(-5.S)
        }.fork {
          c.out.expectDequeueNow(
            new HarnessBundleOut().Lit(
              _.a -> 0xa1.U,
              _.b -> -5.S
            )
          )
        }.join()

        fork {
          c.in.a.enqueueNow(0xb2.U)
        }.fork {
          c.in.b.enqueueNow(-8.S)
        }.fork {
          c.out.expectDequeueNow(
            new HarnessBundleOut().Lit(
              _.a -> 0xb2.U,
              _.b -> -8.S
            )
          )
        }.join()
      }
    }

    it("handles staggered inputs") {
      test(gather).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, a: UInt, b: SInt) =
          fork {
            c.in.a.enqueue(a)
          }.fork {
            monitor {
              (1 to delay) foreach { _ =>
                c.in.a.ready.expect(false.B)
                c.clock.step()
              }
              c.in.a.ready.expect(true.B)
            }
          }.fork {
            c.clock.step(delay)
            c.in.b.enqueueNow(b)
          }.fork {
            (1 to delay) foreach { _ =>
              c.out.expectInvalid()
              c.clock.step()
            }
            c.out.expectDequeueNow(
              new HarnessBundleOut().Lit(
                _.a -> a,
                _.b -> b
              )
            )
          }.join()

        doIter(1, 0xa1.U, -5.S)
        doIter(2, 0xb4.U, -8.S)
      }
    }
  }

  describe("Gather factory") {
    class Harness extends Module {
      val in = IO(Flipped(new HarnessBundleIn))
      val out = IO(Decoupled(new HarnessBundleOut))

      val flipped = new HarnessBundleIn().makeBi(
        _.a -> in.a,
        _.b -> in.b
      )

      out <> Gather(new HarnessBundleOut, flipped)
    }

    def initPorts(c: Harness) = {
      c.in.a.initSource().setSourceClock(c.clock)
      c.in.b.initSource().setSourceClock(c.clock)
      c.out.initSink().setSinkClock(c.clock)
    }

    it("should instantiate properly") {
      test(new Harness()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        fork {
          c.in.a.enqueueNow(0xa1.U)
        }.fork {
          c.in.b.enqueueNow(-5.S)
        }.fork {
          c.out.expectDequeueNow(
            new HarnessBundleOut().Lit(
              _.a -> 0xa1.U,
              _.b -> -5.S
            )
          )
        }.join()

        fork {
          c.in.a.enqueueNow(0xb2.U)
        }.fork {
          c.in.b.enqueueNow(-8.S)
        }.fork {
          c.out.expectDequeueNow(
            new HarnessBundleOut().Lit(
              _.a -> 0xb2.U,
              _.b -> -8.S
            )
          )
        }.join()
      }
    }
  }
}
