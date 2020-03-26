package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.model.JadeIdentifier

case class JadeTable(
  name: JadeIdentifier,
  columns: Set[JadeColumn],
  primaryKey: Set[JadeIdentifier],
  partitionMode: JadePartitionMode,
  datePartitionOptions: Option[JadeDatePartitionOptions],
  intPartitionOptions: Option[JadeIntPartitionOptions]
)

object JadeTable {
  implicit val encoder: Encoder[JadeTable] = deriveEncoder
}
