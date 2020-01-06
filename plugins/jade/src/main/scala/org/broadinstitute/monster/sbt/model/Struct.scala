package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a model for denormalized data within a Jade column.
  *
  * @param name name of the model. Used for cross-linking from columns in
  *             top-level tables
  * @param fields fields contained by the model
  */
case class Struct(
  name: JadeIdentifier,
  fields: Vector[SimpleColumn]
)

object Struct {
  implicit val decoder: Decoder[Struct] = deriveDecoder(renaming.snakeCase, true, None)
}
