package com.alterationx10.bson

import org.bson.{BsonDocument, BsonElement, BsonValue}

import java.util
import scala.annotation.targetName
import scala.jdk.CollectionConverters.*
import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror

trait BsonEncoder[A] {
  def toBson(a: A): BsonValue
}

object BsonEncoder {

  private[bson] inline def summonEncoders[T <: Tuple]: List[BsonEncoder[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[BsonEncoder[t]] :: summonEncoders[ts]
    }
  }

  private[bson] inline def buildDocument[A](
      a: A,
      encoders: List[BsonEncoder[?]]
  ): BsonDocument = {

    val labels: Iterator[String] =
      a.asInstanceOf[Product].productElementNames

    val values: Iterator[?] =
      a.asInstanceOf[Product].productIterator

    val elements: util.List[BsonElement] =
      labels
        .zip(encoders.zip(values))
        .map { l_ev =>
          new BsonElement(
            l_ev._1,
            l_ev._2._1.asInstanceOf[BsonEncoder[Any]].toBson(l_ev._2._2)
          )
        }
        .toList
        .asJava

    new BsonDocument(elements)
  }

  inline given derived[A <: Product](using m: Mirror.Of[A]): BsonEncoder[A] = {
    lazy val encoders = summonEncoders[m.MirroredElemTypes]
    inline m match {
      case _: Mirror.SumOf[A]     =>
        throw new Exception(
          "Auto derivation is not supported for Sum types. Please create them explicitly as needed."
        )
      case _: Mirror.ProductOf[A] =>
        (a: A) => buildDocument(a, encoders)
    }
  }

}
