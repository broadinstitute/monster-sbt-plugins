package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

case class JadeSchema(
  tables: Set[JadeTable],
  relationships: Set[JadeRelationship]
)

object JadeSchema {
  implicit val encoder: Encoder[JadeSchema] = deriveEncoder
}
