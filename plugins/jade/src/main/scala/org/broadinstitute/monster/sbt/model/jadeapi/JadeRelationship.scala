package org.broadinstitute.monster.sbt.model.jadeapi

import java.util.UUID

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

case class JadeRelationship(
  name: UUID,
  from: JadeRelationshipRef,
  to: JadeRelationshipRef
)

object JadeRelationship {
  implicit val encoder: Encoder[JadeRelationship] = deriveEncoder
}
