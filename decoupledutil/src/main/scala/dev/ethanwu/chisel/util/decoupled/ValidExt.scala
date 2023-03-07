package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.util._

/** Mirror of methods on [[ReadyValidIO]] for [[Valid]]
  */
object ValidExt {
  implicit class ValidMethods[T <: Data](target: Valid[T]) {

    /** Push dat onto the output bits of this interface to let the consumer know
      * it has happened.
      * @param dat
      *   the values to assign to bits.
      * @return
      *   dat.
      */
    def enq(dat: T): T = {
      target.valid := true.B
      target.bits := dat
      dat
    }

    /** Indicate no enqueue occurs. Valid is set to false, and bits are
      * connected to an uninitialized wire.
      */
    def noenq(): Unit = {
      target.valid := false.B
      target.bits := DontCare
    }
  }

}
