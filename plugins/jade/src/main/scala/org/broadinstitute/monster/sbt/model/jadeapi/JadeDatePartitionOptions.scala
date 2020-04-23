package org.broadinstitute.monster.sbt.model.jadeapi

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.broadinstitute.monster.sbt.model.JadeIdentifier

case class JadeDatePartitionOptions(column: JadeIdentifier)

object JadeDatePartitionOptions {

  val IngestDate: JadeDatePartitionOptions =
    JadeDatePartitionOptions(new JadeIdentifier("datarepo_ingest_date"))

  implicit val encoder: Encoder[JadeDatePartitionOptions] = deriveEncoder
}
