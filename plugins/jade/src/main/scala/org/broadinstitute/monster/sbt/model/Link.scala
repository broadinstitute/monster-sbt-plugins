package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Pointer to a table/column pair in a Jade dataset.
  *
  * @param tableName name of the pointed-to table
  * @param columnName name of the pointed-to column within `tableName`
  */
case class Link(
  tableName: JadeIdentifier,
  columnName: JadeIdentifier
)

object Link {

  implicit val decoder: Decoder[Link] =
    deriveDecoder(renaming.snakeCase, true, None)
}
