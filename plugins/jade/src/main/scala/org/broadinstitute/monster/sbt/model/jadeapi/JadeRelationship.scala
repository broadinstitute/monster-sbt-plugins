package org.broadinstitute.monster.sbt.model.jadeapi

import java.util.UUID

import io.circe.{Encoder, Json}
import io.circe.derivation.deriveEncoder

case class JadeRelationship(
  from: JadeRelationshipRef,
  to: JadeRelationshipRef
)

object JadeRelationship {

  // Jade requires that relationships have names, so we generate
  // fake ones. THIS IS NOT DETERMINISTIC.
  implicit val encoder: Encoder[JadeRelationship] =
    deriveEncoder.mapJsonObject { base =>
      base.add("name", Json.fromString(UUID.randomUUID().toString))
    }
}
