package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a column in a Jade table.
  *
  * @param name name of the column. Will propogate to the underlying BigQuery schema.
  * @param datatype type of data stored by the column
  * @param `type` type of the column
  * @param links links from the column to other table/column pairs in the dataset
  */
case class JadeColumn(
  name: JadeIdentifier,
  datatype: JadeDatatype,
  `type`: JadeColumnType = JadeColumnType.Optional,
  links: Vector[JadeLink] = Vector.empty
)

object JadeColumn {

  implicit val decoder: Decoder[JadeColumn] =
    deriveDecoder(renaming.snakeCase, true, None)
}
