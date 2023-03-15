package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import dev.ethanwu.chisel.util.test._
import dev.ethanwu.chisel.util.decoupled.MakeBundle.BundleExt
import org.scalatest.funspec.AnyFunSpec

class BarrierSpec extends AnyFunSpec with ChiselScalatestTester {
  class HarnessBundle extends Bundle {
    val a = Decoupled(UInt(8.W))
    val b = Decoupled(SInt(4.W))
  }

  describe("Barrier") {

    def barrier = new Barrier(new HarnessBundle)

    def initPorts(c: Barrier[HarnessBundle]) = {
      c.in.a.initSource().setSourceClock(c.clock)
      c.in.b.initSource().setSourceClock(c.clock)
      c.out.a.initSink().setSinkClock(c.clock)
      c.out.b.initSink().setSinkClock(c.clock)
    }

    it("transfers all on the same cycle") {
      test(barrier).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        fork {
          c.in.a.enqueueNow(0xa1.U)
        }.fork {
          c.in.b.enqueueNow(-5.S)
        }.fork {
          c.out.a.expectDequeueNow(0xa1.U)
        }.fork {
          c.out.b.expectDequeueNow(-5.S)
        }.join()

        fork {
          c.in.a.enqueueNow(0xb2.U)
        }.fork {
          c.in.b.enqueueNow(-8.S)
        }.fork {
          c.out.a.expectDequeueNow(0xb2.U)
        }.fork {
          c.out.b.expectDequeueNow(-8.S)
        }.join()
      }
    }

    it("transfers all some cycle later") {
      test(barrier).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, a: UInt, b: SInt) =
          fork {
            c.in.a.enqueue(a)
          }.fork {
            c.in.b.enqueue(b)
          }.fork {
            monitor {
              (1 to delay) foreach { _ =>
                c.in.a.ready.expect(false.B)
                c.clock.step()
              }
              c.in.a.ready.expect(true.B)
            }.joinAndStep(c.clock)
          }.fork {
            monitor {
              (1 to delay) foreach { _ =>
                c.in.b.ready.expect(false.B)
                c.clock.step()
              }
              c.in.b.ready.expect(true.B)
            }.joinAndStep(c.clock)
          }.fork {
            c.clock.step(delay)
            fork {
              c.out.a.expectDequeueNow(a)
            }.fork {
              c.out.b.expectDequeueNow(b)
            }.join()
          }.join()

        doIter(1, 0xa1.U, -5.S)
        doIter(2, 0xb4.U, -8.S)
      }
    }

    it("handles staggered inputs and synchronized outputs") {
      test(barrier).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, a: UInt, b: SInt) =
          fork {
            fork {
              c.in.a.enqueue(a)
            }.fork {
              monitor {
                (1 to delay) foreach { _ =>
                  c.in.a.ready.expect(false.B)
                  c.clock.step()
                }
                c.in.a.ready.expect(true.B)
              }.joinAndStep(c.clock)
            }.join()
          }.fork {
            c.clock.step(delay)
            c.in.b.enqueueNow(b)
          }.fork {
            c.out.a.expectInvalid()
            c.out.b.expectInvalid()
          }.fork {
            c.clock.step(delay)
            fork {
              c.out.a.expectDequeueNow(a)
            }.fork {
              c.out.b.expectDequeueNow(b)
            }.join()
          }.join()

        doIter(1, 0xa1.U, -5.S)
        doIter(2, 0xb4.U, -8.S)
      }
    }

    it("handles synchronized inputs and staggered outputs") {
      test(barrier).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, a: UInt, b: SInt) =
          fork {
            fork {
              c.in.a.enqueue(a)
            }.fork {
              monitor {
                (1 to delay) foreach { _ =>
                  c.in.a.ready.expect(false.B)
                  c.clock.step()
                }
                c.in.a.ready.expect(true.B)
              }.joinAndStep(c.clock)
            }.join()
          }.fork {
            fork {
              c.in.b.enqueue(b)
            }.fork {
              monitor {
                (1 to delay) foreach { _ =>
                  c.in.b.ready.expect(false.B)
                  c.clock.step()
                }
                c.in.b.ready.expect(true.B)
              }.joinAndStep(c.clock)
            }.join()
          }.fork {
            c.out.a.expectDequeueNow(a)
            (1 to delay) foreach { _ =>
              c.out.a.expectInvalid()
              c.clock.step()
            }
          }.fork {
            c.clock.step(delay)
            c.out.b.expectDequeueNow(b)
          }.join()

        doIter(1, 0xa1.U, -5.S)
        doIter(2, 0xb4.U, -8.S)
      }
    }

    it("handles staggered inputs and staggered outputs") {
      test(barrier).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, a: UInt, b: SInt) =
          fork {
            fork {
              c.in.a.enqueue(a)
            }.fork {
              monitor {
                (1 to (delay * 2)) foreach { _ =>
                  c.in.a.ready.expect(false.B)
                  c.clock.step()
                }
                c.in.a.ready.expect(true.B)
              }.joinAndStep(c.clock)
            }.join()
          }.fork {
            c.clock.step(delay)
            fork {
              c.in.b.enqueue(b)
            }.fork {
              monitor {
                (1 to delay) foreach { _ =>
                  c.in.b.ready.expect(false.B)
                  c.clock.step()
                }
                c.in.b.ready.expect(true.B)
              }.joinAndStep(c.clock)
            }.join()
          }.fork {
            c.clock.step(delay)
            fork {
              c.out.a.expectDequeueNow(a)
              (1 to delay) foreach { _ =>
                c.out.a.expectInvalid()
                c.clock.step()
              }
            }.fork {
              c.clock.step(delay)
              c.out.b.expectDequeueNow(b)
            }.join()
          }.join()

        doIter(1, 0xa1.U, -5.S)
        doIter(2, 0xb4.U, -8.S)
      }
    }
  }

  describe("Barrier factory") {
    describe("on bundles") {
      class Harness extends Module {
        val in = IO(Flipped(new HarnessBundle()))
        val out = IO(new HarnessBundle())

        /* TODO: not sure why this works tbh... make is connecting
         * rhs(out) -> lhs(out) and lhs(in) -> rhs(in) */
        val flipped = new HarnessBundle().makeBi(
          _.a -> DecoupledOut(in.a),
          _.b -> DecoupledOut(in.b)
        )

        out <> Barrier(flipped)
      }

      def initPorts(c: Harness) = {
        c.in.a.initSource().setSourceClock(c.clock)
        c.in.b.initSource().setSourceClock(c.clock)
        c.out.a.initSink().setSinkClock(c.clock)
        c.out.b.initSink().setSinkClock(c.clock)
      }

      it("should instantiate properly") {
        test(new Harness()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
          initPorts(c)

          fork {
            c.in.a.enqueueNow(0xa1.U)
          }.fork {
            c.in.b.enqueueNow(-5.S)
          }.fork {
            c.out.a.expectDequeueNow(0xa1.U)
          }.fork {
            c.out.b.expectDequeueNow(-5.S)
          }.join()

          fork {
            c.in.a.enqueueNow(0xb2.U)
          }.fork {
            c.in.b.enqueueNow(-8.S)
          }.fork {
            c.out.a.expectDequeueNow(0xb2.U)
          }.fork {
            c.out.b.expectDequeueNow(-8.S)
          }.join()
        }
      }
    }

    describe("on 2 tuples") {
      class Harness extends Module {
        val in = IO(Flipped(new HarnessBundle()))
        val out = IO(new HarnessBundle())

        val (outA, outB) = Barrier.group(in.a, in.b)
        out.a <> outA
        out.b <> outB
      }

      def initPorts(c: Harness) = {
        c.in.a.initSource().setSourceClock(c.clock)
        c.in.b.initSource().setSourceClock(c.clock)
        c.out.a.initSink().setSinkClock(c.clock)
        c.out.b.initSink().setSinkClock(c.clock)
      }

      it("should instantiate properly") {
        test(new Harness()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
          initPorts(c)

          fork {
            c.in.a.enqueueNow(0xa1.U)
          }.fork {
            c.in.b.enqueueNow(-5.S)
          }.fork {
            c.out.a.expectDequeueNow(0xa1.U)
          }.fork {
            c.out.b.expectDequeueNow(-5.S)
          }.join()

          fork {
            c.in.a.enqueueNow(0xb2.U)
          }.fork {
            c.in.b.enqueueNow(-8.S)
          }.fork {
            c.out.a.expectDequeueNow(0xb2.U)
          }.fork {
            c.out.b.expectDequeueNow(-8.S)
          }.join()
        }
      }
    }

    describe("on 3 tuples") {
      class HarnessBundle extends Bundle {
        val a = Decoupled(UInt(8.W))
        val b = Decoupled(SInt(4.W))
        val c = Decoupled(Bool())
      }

      class Harness extends Module {
        val in = IO(Flipped(new HarnessBundle()))
        val out = IO(new HarnessBundle())

        val (outA, outB, outC) = Barrier.group(in.a, in.b, in.c)
        out.a <> outA
        out.b <> outB
        out.c <> outC
      }

      def initPorts(c: Harness) = {
        c.in.a.initSource().setSourceClock(c.clock)
        c.in.b.initSource().setSourceClock(c.clock)
        c.in.c.initSource().setSourceClock(c.clock)
        c.out.a.initSink().setSinkClock(c.clock)
        c.out.b.initSink().setSinkClock(c.clock)
        c.out.c.initSink().setSinkClock(c.clock)
      }

      it("should instantiate properly") {
        test(new Harness()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
          initPorts(c)

          fork {
            c.in.a.enqueueNow(0xa1.U)
          }.fork {
            c.in.b.enqueueNow(-5.S)
          }.fork {
            c.in.c.enqueueNow(true.B)
          }.fork {
            c.out.a.expectDequeueNow(0xa1.U)
          }.fork {
            c.out.b.expectDequeueNow(-5.S)
          }.fork {
            c.out.c.expectDequeueNow(true.B)
          }.join()

          fork {
            c.in.a.enqueueNow(0xb2.U)
          }.fork {
            c.in.b.enqueueNow(-8.S)
          }.fork {
            c.in.c.enqueueNow(true.B)
          }.fork {
            c.out.a.expectDequeueNow(0xb2.U)
          }.fork {
            c.out.b.expectDequeueNow(-8.S)
          }.fork {
            c.out.c.expectDequeueNow(true.B)
          }.join()
        }
      }
    }
  }
}
