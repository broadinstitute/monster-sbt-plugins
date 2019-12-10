package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.model.JadeIdentifier

case class JadeRelationshipRef(
  table: JadeIdentifier,
  column: JadeIdentifier,
  cardinality: String
)

object JadeRelationshipRef {
  implicit val encoder: Encoder[JadeRelationshipRef] = deriveEncoder
}
