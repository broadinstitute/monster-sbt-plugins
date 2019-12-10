package org.broadinstitute.monster.sbt.model.jadeapi

import java.security.MessageDigest

import io.circe.{Encoder, Json}
import io.circe.derivation.deriveEncoder
import io.circe.syntax._

case class JadeRelationship(
  from: JadeRelationshipRef,
  to: JadeRelationshipRef
)

object JadeRelationship {
  /*
   * Jade requires that relationships have names, so we generate
   * a relationship's name by hashing its JSONified contents.
   */
  private val digest = MessageDigest.getInstance("SHA-256")

  implicit val encoder: Encoder[JadeRelationship] =
    deriveEncoder.mapJsonObject { base =>
      val nameBytes = digest.digest(base.asJson.noSpaces.getBytes)
      base.add("name", Json.fromString(new String(nameBytes)))
    }
}
