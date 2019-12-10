package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.{Encoder, Json}
import io.circe.derivation.deriveEncoder

case class JadeSchema(
  tables: Seq[JadeTable],
  relationships: Seq[JadeRelationship]
)

object JadeSchema {

  implicit val encoder: Encoder[JadeSchema] =
    deriveEncoder.mapJsonObject(_.add("assets", Json.arr()))
}
