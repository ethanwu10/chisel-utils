package dev.ethanwu.chisel.util.decoupled

import chisel3._
import chisel3.experimental.conversions._
import chisel3.experimental.{HWTuple2, HWTuple3}
import chisel3.util._

class Gather[T <: Aggregate, U <: Data](
    genIn: T,
    genOut: U,
    mapper: (T, U) => Unit
) extends Module {
  def this(genIn: T, genOut: U, mapper: T => U) = {
    this(genIn, genOut, { (in: T, out: U) => out := mapper(in) })
  }

  def this(genIn: T, genOut: U)(implicit ev: U <:< Aggregate) = {
    this(
      genIn,
      genOut,
      { (in: T, out: U) =>
        (in, out) match {
          case (in: Record, out: Record) =>
            require(
              in.elements.keySet.equals(out.elements.keySet),
              "input and output bundles do not have matching keys"
            )
            in.elements.foreach { case (k, inRaw) =>
              val in = inRaw.asInstanceOf[ReadyValidIO[Data]]
              out.elements(k) := in.bits
            }
          case _ =>
            (in.getElements zip out.getElements) foreach { case (inRaw, out) =>
              val in = inRaw.asInstanceOf[ReadyValidIO[Data]]
              out := in.bits
            }
        }
      }
    )
    require(genIn.getElements.length == genOut.getElements.length)
  }

  val in = IO(Flipped(genIn))
  val out = IO(Decoupled(genOut))

  private val numElts = in.getElements.length

  // Upstream valids
  private val valids = Wire(Vec(numElts, Bool()))
  // Are all upstreams valid? If so, send downstream
  private val allValid = valids.asUInt.andR

  (in.getElements zip valids).foreach { case (raw, valid) =>
    val elt = raw.asInstanceOf[ReadyValidIO[Data]]
    valid := elt.valid
    elt.ready := out.fire
  }
  out.valid := allValid

  mapper(in, out.bits)
}

object Gather {
  private def apply_impl[T <: Aggregate, U <: Data](
      in: T,
      module: Gather[T, U]
  ): DecoupledIO[U] = {
    val gather = module
    gather.in <> in
    gather.out
  }

  def apply[T <: Aggregate, U <: Data](
      outGen: U,
      in: T,
      mapper: (T, U) => Unit
  ): DecoupledIO[U] = {
    apply_impl(in, Module(new Gather(chiselTypeOf(in), outGen, mapper)))
  }

  def apply[T <: Aggregate, U <: Data](
      outGen: U,
      in: T,
      mapper: T => U
  ): DecoupledIO[U] = {
    apply_impl(in, Module(new Gather(chiselTypeOf(in), outGen, mapper)))
  }

  def apply[T <: Aggregate, U <: Aggregate](
      outGen: U,
      in: T
  ): DecoupledIO[U] = {
    apply_impl(in, Module(new Gather(chiselTypeOf(in), outGen)))
  }

  def group[T1 <: Data, T2 <: Data, U <: Data](
      in: (ReadyValidIO[T1], ReadyValidIO[T2]),
      outGen: U
  )(
      mapper: (HWTuple2[ReadyValidIO[T1], ReadyValidIO[T2]], U) => Unit
  ): DecoupledIO[U] = {
    apply(
      outGen,
      tuple2hwtuple(in),
      mapper
    )
  }

  def group[T1 <: Data, T2 <: Data, T3 <: Data, U <: Data](
      in: (ReadyValidIO[T1], ReadyValidIO[T2], ReadyValidIO[T3]),
      outGen: U
  )(
      mapper: (
          HWTuple3[ReadyValidIO[T1], ReadyValidIO[T2], ReadyValidIO[T3]],
          U
      ) => Unit
  ): DecoupledIO[U] = {
    apply(
      outGen,
      tuple3hwtuple(in),
      mapper
    )
  }
}
