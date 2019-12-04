package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Pointer to a table/column pair in a Jade dataset.
  *
  * @param tableName name of the pointed-to table
  * @param columnName name of the pointed-to column within `tableName`
  */
case class JadeLink(
  tableName: JadeIdentifier,
  columnName: JadeIdentifier
)

object JadeLink {

  implicit val decoder: Decoder[JadeLink] =
    deriveDecoder(renaming.snakeCase, true, None)
}
