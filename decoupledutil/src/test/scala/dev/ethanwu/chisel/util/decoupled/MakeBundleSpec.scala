package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chiseltest._
import org.scalatest.funspec.AnyFunSpec
import chisel3.experimental.BundleLiterals._

import MakeBundle._

class MakeBundleSpec extends AnyFunSpec with ChiselScalatestTester {
  describe("MakeBundle") {
    it("works") {
      class HarnessBundle extends Bundle {
        val a = UInt(8.W)
        val b = SInt(4.W)
      }
      class Harness extends Module {
        val in = IO(Input(new HarnessBundle))
        val out = IO(Output(new HarnessBundle))

        out := new HarnessBundle().makeBi(
          _.a -> in.a,
          _.b -> in.b
        )
      }
      test(new Harness) { c =>
        val bundleVal = new HarnessBundle().Lit(_.a -> 0xa4.U, _.b -> -1.S)
        c.in.poke(bundleVal)
        c.out.expect(bundleVal)
      }
    }
  }
}
