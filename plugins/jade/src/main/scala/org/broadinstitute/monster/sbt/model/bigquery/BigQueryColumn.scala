package org.broadinstitute.monster.sbt.model.bigquery

import io.circe.Encoder
import io.circe.derivation.deriveEncoder

case class BigQueryColumn(name: String, `type`: String, mode: String)

object BigQueryColumn {
  implicit val encoder: Encoder[BigQueryColumn] = deriveEncoder
}
