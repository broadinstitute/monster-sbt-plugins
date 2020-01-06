package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a column in a Jade table without nested fields.
  *
  * @param name name of the column. Will propogate to the underlying BigQuery schema.
  * @param datatype type of data stored by the column
  * @param `type` type of the column
  * @param links links from the column to other table/column pairs in the dataset
  */
case class SimpleColumn(
  name: JadeIdentifier,
  datatype: DataType,
  `type`: ColumnType = ColumnType.Optional,
  links: Vector[Link] = Vector.empty
)

object SimpleColumn {

  implicit val decoder: Decoder[SimpleColumn] =
    deriveDecoder(renaming.snakeCase, true, None)
}
