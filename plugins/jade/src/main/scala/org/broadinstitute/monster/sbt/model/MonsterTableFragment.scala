package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

case class MonsterTableFragment(
  name: JadeIdentifier,
  columns: Vector[SimpleColumn] = Vector.empty,
  structColumns: Vector[StructColumn] = Vector.empty,
  tableFragments: Vector[JadeIdentifier] = Vector.empty
) extends ClassSpec

object MonsterTableFragment {

  implicit val decoder: Decoder[MonsterTableFragment] =
    deriveDecoder(renaming.snakeCase, true, None)
}
