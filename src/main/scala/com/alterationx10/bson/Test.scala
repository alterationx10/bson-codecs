package com.alterationx10.bson

import org.bson.BsonString

object Test extends scala.App {

  case class Simple(a: String = "string", b: Int = 42)

  case class Thing(l: Long = 42, s: Simple = Simple()) derives BsonCodec

  val codec: BsonEncoder[Thing] = summon[BsonEncoder[Thing]]

  println(codec.toBson(Thing()))
}
