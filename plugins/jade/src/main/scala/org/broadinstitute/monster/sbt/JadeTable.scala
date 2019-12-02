package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/** TODO */
case class JadeTable(
  name: JadeIdentifier,
  columns: Vector[JadeColumn]
)

object JadeTable {

  implicit val decoder: Decoder[JadeTable] =
    deriveDecoder(renaming.snakeCase, true, None)
}
