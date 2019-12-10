package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a table in a Jade dataset.
  *
  * @param name name of the table. Will propogate to the underlying BigQuery dataset
  * @param columns columns contained by the table
  */
case class MonsterTable(
  name: JadeIdentifier,
  columns: Vector[MonsterColumn]
)

object MonsterTable {

  implicit val decoder: Decoder[MonsterTable] =
    deriveDecoder(renaming.snakeCase, true, None)
}
