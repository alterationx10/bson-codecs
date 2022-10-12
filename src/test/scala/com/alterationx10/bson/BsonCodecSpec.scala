package com.alterationx10.bson

import zio.*
import zio.test.*
import java.util.UUID
import org.bson.types.ObjectId
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.bson.*

object BsonCodecSpec extends ZIOSpecDefault {

  case class Simple(
      _id: ObjectId,
      str: String,
      int: Int,
      lng: Long,
      dbl: Double,
      flt: Float,
      bl: Boolean,
      uuid: UUID,
      whn: Instant
  )

  val id: UUID          = UUID.randomUUID()
  val bid: BsonObjectId = new BsonObjectId()
  val now: Instant      = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  val flt: Float        = 5.4321

  val simple: Simple = Simple(
    _id = bid.getValue(),
    str = "a string",
    int = 42,
    lng = 54321,
    dbl = 12345,
    flt = flt,
    bl = true,
    uuid = id,
    whn = now
  )

  val simpleDoc: BsonDocument = new BsonDocument()
  simpleDoc.put("_id", bid)
  simpleDoc.put("str", new BsonString("a string"))
  simpleDoc.put("int", new BsonInt32(42))
  simpleDoc.put("lng", new BsonInt64(54321))
  simpleDoc.put("dbl", new BsonDouble(12345))
  simpleDoc.put("flt", new BsonDouble(flt))
  simpleDoc.put("bl", new BsonBoolean(true))
  simpleDoc.put("uuid", BsonBinary.apply(id))
  simpleDoc.put("whn", new BsonDateTime(now.toEpochMilli()))

  val simpleCodec: BsonCodec[Simple] = summon[BsonCodec[Simple]]

  val simpleSuite = suite("Simple Case Class")(
    test("convert to bson") {
      assertTrue(
        simpleCodec.toBson(simple) == simpleDoc
      )
    },
    test("convert from bson") {
      assertTrue(
        simpleCodec.fromBson(simpleDoc) == simple
      )
    },
    test("convert to and back from bson") {
      assertTrue(
        simpleCodec.fromBson(simpleCodec.toBson(simple)) == simple
      )
    }
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("BsonCodecSpec")(
      simpleSuite
    )

}
