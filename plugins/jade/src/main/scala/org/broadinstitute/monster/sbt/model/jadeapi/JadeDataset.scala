package org.broadinstitute.monster.sbt.model.jadeapi

import java.util.UUID

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.model.JadeIdentifier

case class JadeDataset(
  name: JadeIdentifier,
  description: String,
  defaultProfileId: UUID,
  schema: JadeSchema
)

object JadeDataset {
  implicit val encoder: Encoder[JadeDataset] = deriveEncoder
}
