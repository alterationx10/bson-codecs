package com.alterationx10.bson

import org.bson.BsonValue

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror
import scala.deriving.Mirror.ProductOf
import scala.util.Try

trait BsonDecoder[A] {
  def fromBson(b: BsonValue): A
}

object BsonDecoder {

  private inline def summonDecoders[A <: Tuple]: List[BsonDecoder[?]] = {
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[BsonDecoder[t]] :: summonDecoders[ts]
  }

  private[bson] inline def summonLabels[A <: Tuple]: List[String] = {
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value
          .asInstanceOf[String] :: summonLabels[ts]
  }

  private[bson] inline def buildProduct[A](
      p: Mirror.ProductOf[A],
      b: BsonValue,
      labels: List[String],
      decoders: List[BsonDecoder[?]]
  ) = {
    {
      val consArr = labels
        .zip(decoders)
        .map { kv =>
          val anyDecoder = kv._2
          anyDecoder
            .fromBson(b.asDocument().get(kv._1))
        }
        .toArray

      p.fromProduct(Tuple.fromArray[Any](consArr))
    }
  }

  inline given derived[A <: Product](using m: Mirror.Of[A]): BsonDecoder[A] = {
    lazy val decoders = summonDecoders[m.MirroredElemTypes]
    lazy val labels   = summonLabels[m.MirroredElemLabels]
    inline m match {
      case _: Mirror.SumOf[A] =>
        throw new Exception(
          "Auto derivation is not supported for Sum types. Please create them explicitly as needed."
        )
      case p: ProductOf[A]    =>
        (b: BsonValue) => buildProduct(p, b, labels, decoders)
    }
  }

}
