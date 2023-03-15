package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO}
import chiseltest._
import dev.ethanwu.chisel.util.test.monitor
import org.scalatest.funspec.AnyFunSpec

class SplitterSpec extends AnyFunSpec with ChiselScalatestTester {
  describe("Splitter") {
    def splitter(n: Int) = new Splitter(Decoupled(UInt(8.W)), n)
    def initPorts(c: Splitter[DecoupledIO[UInt]]) = {
      c.in.initSource().setSourceClock(c.clock)
      c.out.foreach { _.initSink().setSinkClock(c.clock) }
    }

    it("transfers all on the same cycle") {
      test(splitter(3)) { c =>
        initPorts(c)

        def doIter(x: UInt) = {
          var f = fork {
            c.in.enqueueNow(x)
          }
          c.out.foreach { s =>
            f = f.fork {
              s.expectDequeueNow(x)
            }
          }
          f.join()
        }

        doIter(0xa4.U)
        doIter(0xb1.U)
      }
    }

    it("transfers all some cycle later") {
      test(splitter(3)) { c =>
        initPorts(c)

        def doIter(delay: Int, x: UInt) = {
          var f = fork {
            c.in.enqueue(x)
          }.fork {
            monitor {
              (1 to delay) foreach { _ =>
                c.in.ready.expect(false.B)
                c.clock.step()
              }
              c.in.ready.expect(true.B)
            }.joinAndStep(c.clock)
          }

          c.out.foreach { s =>
            f = f.fork {
              c.clock.step(delay)
              s.expectDequeueNow(x)
            }
          }

          f.join()
        }

        doIter(1, 0xa4.U)
        doIter(2, 0xb1.U)
      }
    }

    it("handles staggered outputs") {
      test(splitter(3)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(delay: Int, x: UInt) = {
          var f = fork {
            c.in.enqueue(x)
          }.fork {
            monitor {
              (1 to (delay * (c.out.length - 1))) foreach { _ =>
                c.in.ready.expect(false.B)
                c.clock.step()
              }
              c.in.ready.expect(true.B)
            }.joinAndStep(c.clock)
          }

          c.out.zipWithIndex.foreach { case (s, i) =>
            f = f.fork {
              c.clock.step(i * delay)
              s.expectDequeueNow(x)
              (1 to (delay * (c.out.length - 1))) foreach { _ =>
                s.expectInvalid()
                c.clock.step()
              }
            }
          }

          f.join()
        }

        doIter(1, 0xa4.U)
        doIter(2, 0xb1.U)
      }
    }
  }

  describe("Splitter factory") {
    it("should instantiate properly") {
      class Harness extends Module {
        val in = IO(Flipped(Decoupled(UInt(8.W))))
        val out = IO(Vec(3, Decoupled(UInt(8.W))))

        out <> Splitter(3, DecoupledOut(in))
      }
      def initPorts(c: Harness) = {
        c.in.initSource().setSourceClock(c.clock)
        c.out.foreach { _.initSink().setSinkClock(c.clock) }
      }

      test(new Harness()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        initPorts(c)

        def doIter(x: UInt) = {
          var f = fork {
            c.in.enqueueNow(x)
          }
          c.out.foreach { s =>
            f = f.fork {
              s.expectDequeueNow(x)
            }
          }
          f.join()
        }

        doIter(0xa4.U)
        doIter(0xb1.U)
      }
    }
  }
}
