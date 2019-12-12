package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.{Encoder, Json}
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.model.JadeIdentifier

case class JadeRelationshipRef(
  table: JadeIdentifier,
  column: JadeIdentifier
)

object JadeRelationshipRef {
  /**
    * Constant cardinality to use for all Jade relationships.
    *
    * Jade validates that this string matches an enum list but doesn't use the
    * value for anything else, so we don't bother specifying / deriving it.
    */
  private val cardinality = Json.fromString("many")

  implicit val encoder: Encoder[JadeRelationshipRef] =
    deriveEncoder.mapJsonObject(_.add("cardinality", cardinality))
}
