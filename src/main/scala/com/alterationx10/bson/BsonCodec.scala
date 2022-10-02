package com.alterationx10.bson

import org.bson.codecs.{
  BsonDocumentCodec,
  BsonValueCodec,
  Codec,
  DecoderContext,
  EncoderContext
}
import org.bson.*
import org.bson.types.ObjectId

import java.time.{Instant, ZoneOffset}
import java.util
import java.util.UUID
import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

trait BsonCodec[A] extends BsonEncoder[A], BsonDecoder[A]

object BsonCodec {

  given BsonCodec[String] = new BsonCodec[String]:
    override def toBson(a: String): BsonValue   = new BsonString(a)
    override def fromBson(b: BsonValue): String = b.asString().getValue

  given BsonCodec[Int] = new BsonCodec[Int]:
    override def toBson(a: Int): BsonValue   = new BsonInt32(a)
    override def fromBson(b: BsonValue): Int = b.asInt32().getValue

  given BsonCodec[Long] = new BsonCodec[Long]:
    override def toBson(a: Long): BsonValue   = new BsonInt64(a)
    override def fromBson(b: BsonValue): Long = b.asInt64().getValue

  given BsonCodec[Double] = new BsonCodec[Double]:
    override def toBson(a: Double): BsonValue   = new BsonDouble(a)
    override def fromBson(b: BsonValue): Double = b.asDouble().getValue

  given BsonCodec[Boolean] = new BsonCodec[Boolean]:
    override def toBson(a: Boolean): BsonValue   = new BsonBoolean(a)
    override def fromBson(b: BsonValue): Boolean = b.asBoolean().getValue

  given BsonCodec[Array[Byte]] = new BsonCodec[Array[Byte]]:
    override def toBson(a: Array[Byte]): BsonValue   = new BsonBinary(a)
    override def fromBson(b: BsonValue): Array[Byte] = b.asBinary().getData

  given BsonCodec[ObjectId] = new BsonCodec[ObjectId]:
    override def toBson(a: ObjectId): BsonValue   = new BsonObjectId(a)
    override def fromBson(b: BsonValue): ObjectId = b.asObjectId().getValue

  given BsonCodec[UUID] = new BsonCodec[UUID]:
    override def toBson(a: UUID): BsonValue   = new BsonBinary(a)
    override def fromBson(b: BsonValue): UUID = b.asBinary().asUuid()

  given BsonCodec[Instant] = new BsonCodec[Instant]:
    override def toBson(a: Instant): BsonValue   = new BsonDateTime(
      a.toEpochMilli
    )
    override def fromBson(b: BsonValue): Instant =
      Instant.ofEpochMilli(b.asDateTime().getValue)

  given [A: BsonCodec]: BsonCodec[Option[A]] = new BsonCodec[Option[A]]:
    override def fromBson(b: BsonValue): Option[A] = if (b.isNull) then
      Option.empty[A]
    else Some(summon[BsonCodec[A]].fromBson(b))
    override def toBson(a: Option[A]): BsonValue   =
      a.map(summon[BsonCodec[A]].toBson).getOrElse(BsonNull.VALUE)

  given [A: BsonCodec]: BsonCodec[List[A]] = new BsonCodec[List[A]]:
    override def toBson(a: List[A]): BsonValue   =
      new BsonArray(a.map(summon[BsonCodec[A]].toBson).asJava)
    override def fromBson(b: BsonValue): List[A] =
      b.asArray().asScala.map(summon[BsonCodec[A]].fromBson).toList

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

  def codecOf[A <: Product](using
      codec: BsonCodec[A],
      ct: ClassTag[A]
  ): Codec[A] = new Codec[A] {
    override def decode(
        reader: BsonReader,
        decoderContext: DecoderContext
    ): A = {
      val bsonCode = new BsonDocumentCodec()
      codec.fromBson(bsonCode.decode(reader, decoderContext))
    }

    override def encode(
        writer: BsonWriter,
        value: A,
        encoderContext: EncoderContext
    ): Unit = {
      val bsonCode = new BsonDocumentCodec()
      bsonCode.encode(
        writer,
        codec.toBson(value).asDocument(),
        encoderContext
      )
    }

    override def getEncoderClass: Class[A] =
      ct.runtimeClass.asInstanceOf[Class[A]]
  }

}
