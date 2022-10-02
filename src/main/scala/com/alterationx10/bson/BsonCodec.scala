package com.alterationx10.bson

import org.bson.codecs.{
  BsonDocumentCodec,
  Codec,
  DecoderContext,
  EncoderContext
}
import org.bson.*

import java.util
import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

trait BsonCodec[A] extends BsonEncoder[A], BsonDecoder[A]

object BsonCodec {

  given BsonCodec[String] = new BsonCodec[String]:
    override def toBson(a: String): BsonValue = new BsonString(a)

    override def fromBson(b: BsonValue): String = b.asString().getValue

  given BsonCodec[Int] = new BsonCodec[Int]:
    override def fromBson(b: BsonValue): Int = b.asInt32().getValue

    override def toBson(a: Int): BsonValue = new BsonInt32(a)

  given BsonCodec[Long] = new BsonCodec[Long]:
    override def fromBson(b: BsonValue): Long = b.asInt64().getValue
    override def toBson(a: Long): BsonValue   = new BsonInt64(a)

  given [T: BsonCodec]: BsonCodec[Option[T]] = new BsonCodec[Option[T]]:
    override def fromBson(b: BsonValue): Option[T] = if (b.isNull) then
      Option.empty[T]
    else Some(summon[BsonCodec[T]].fromBson(b))
    override def toBson(a: Option[T]): BsonValue   =
      a.map(summon[BsonCodec[T]].toBson).getOrElse(BsonNull.VALUE)

  given [T: BsonCodec, S <: Seq[T] | Set[T]]: BsonCodec[S] = new BsonCodec[S]:
    override def fromBson(b: BsonValue): S = ???
    override def toBson(a: S): BsonValue = ???

  private inline def summonCodecs[T <: Tuple]: List[BsonCodec[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[BsonCodec[t]] :: summonCodecs[ts]
    }
  }

  inline given derived[A <: Product](using m: Mirror.Of[A]): BsonCodec[A] = {
    lazy val codecs = summonCodecs[m.MirroredElemTypes]
    lazy val labels = BsonDecoder.summonLabels[m.MirroredElemLabels]
    inline m match {
      case _: Mirror.SumOf[A]     =>
        throw new Exception(
          "Auto derivation is not supported for Sum types. Please create them explicitly as needed."
        )
      case p: Mirror.ProductOf[A] =>
        new BsonCodec[A]:
          override def toBson(a: A): BsonValue   =
            BsonEncoder.buildDocument(a, codecs)
          override def fromBson(b: BsonValue): A =
            BsonDecoder.buildProduct(
              p,
              b,
              labels,
              codecs
            )
    }
  }

  def codecOf[T](using
      codec: BsonCodec[T],
      ct: ClassTag[T]
  ): Codec[T] = new Codec[T] {
    override def decode(
        reader: BsonReader,
        decoderContext: DecoderContext
    ): T = {
      val bsonCode = new BsonDocumentCodec()
      codec.fromBson(bsonCode.decode(reader, decoderContext))
    }

    override def encode(
        writer: BsonWriter,
        value: T,
        encoderContext: EncoderContext
    ): Unit = {
      val bsonCode = new BsonDocumentCodec()
      bsonCode.encode(
        writer,
        codec.toBson(value).asDocument(),
        encoderContext
      )
    }

    override def getEncoderClass: Class[T] =
      ct.runtimeClass.asInstanceOf[Class[T]]
  }

}
