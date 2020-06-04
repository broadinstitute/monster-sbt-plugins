package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Description of a table in a Jade dataset.
  *
  * @param name name of the table. Will propogate to the underlying BigQuery dataset
  * @param columns columns contained by the table which do not contain nested fields
  * @param structColumns columns contained by the table which contain nested fields
  * @param composeTables names of other tables which should be "merged" with the columns
  *                      in this table to form the final schema
  * @param partitioning the partitioning strategy to use for the table when stored in BigQuery
  */
case class MonsterTable(
  name: JadeIdentifier,
  columns: Vector[SimpleColumn] = Vector.empty,
  structColumns: Vector[StructColumn] = Vector.empty,
  composeTables: Vector[JadeIdentifier] = Vector.empty,
  partitioning: PartitionMode = PartitionMode.IngestDate
)

object MonsterTable {

  implicit val decoder: Decoder[MonsterTable] =
    deriveDecoder(renaming.snakeCase, true, None)
}
