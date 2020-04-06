package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

case class JadeRelationship(
  name: String,
  from: JadeRelationshipRef,
  to: JadeRelationshipRef
)

object JadeRelationship {

  def apply(from: JadeRelationshipRef, to: JadeRelationshipRef): JadeRelationship =
    JadeRelationship(
      name = s"from_${from.table.id}.${from.column.id}_to_${to.table.id}.${to.column.id}",
      from = from,
      to = to
    )

  implicit val encoder: Encoder[JadeRelationship] = deriveEncoder
}
