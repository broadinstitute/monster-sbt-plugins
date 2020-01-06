package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a column in a Jade table which will have nested fields.
  *
  * @param name name of the column. Will propogate to the underlying BigQuery schema
  * @param structName name of the struct model that should be enforced within the
  *                   column's contents. Note that this model can be enforced within
  *                   generated Scala classes, but not the Jade schema itself
  * @param `type` type of the column
  */
case class StructColumn(
  name: JadeIdentifier,
  structName: JadeIdentifier,
  `type`: ColumnType = ColumnType.Optional
)

object StructColumn {

  implicit val decoder: Decoder[StructColumn] =
    deriveDecoder(renaming.snakeCase, true, None)
}
