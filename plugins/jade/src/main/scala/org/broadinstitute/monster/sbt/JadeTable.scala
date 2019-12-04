package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a table in a Jade dataset.
  *
  * @param name name of the table. Will propogate to the underlying BigQuery dataset
  * @param columns columns contained by the table
  */
case class JadeTable(
  name: JadeIdentifier,
  columns: Vector[JadeColumn]
)

object JadeTable {

  implicit val decoder: Decoder[JadeTable] =
    deriveDecoder(renaming.snakeCase, true, None)
}
