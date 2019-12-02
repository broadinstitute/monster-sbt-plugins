package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/** TODO */
case class JadeLink(
  tableName: JadeIdentifier,
  columnName: JadeIdentifier
)

object JadeLink {

  implicit val decoder: Decoder[JadeLink] =
    deriveDecoder(renaming.snakeCase, true, None)
}
