package org.broadinstitute.monster.sbt

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/** TODO */
case class JadeColumn(
  name: JadeIdentifier,
  datatype: JadeDatatype,
  modifier: JadeColumnModifier = JadeColumnModifier.Normal,
  links: Vector[JadeLink] = Vector.empty
)

object JadeColumn {

  implicit val decoder: Decoder[JadeColumn] =
    deriveDecoder(renaming.snakeCase, true, None)
}
