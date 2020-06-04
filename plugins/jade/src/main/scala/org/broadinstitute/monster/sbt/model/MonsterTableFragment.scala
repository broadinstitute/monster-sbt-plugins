package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Partial description of a Jade table.
  *
  * @param name name of the fragment. Used to reference this fragment from table definitions
  *             or other fragments.
  * @param columns columns included in the fragment
  * @param structColumns structs included in the fragment
  */
case class MonsterTableFragment(
  name: JadeIdentifier,
  columns: Vector[SimpleColumn] = Vector.empty,
  structColumns: Vector[StructColumn] = Vector.empty
) extends ClassSpec {
  // Could be nice to support arbitrary nesting, but the complexity isn't worth it for now.
  override val tableFragments: Vector[JadeIdentifier] = Vector.empty
}

object MonsterTableFragment {

  implicit val decoder: Decoder[MonsterTableFragment] =
    deriveDecoder(renaming.snakeCase, true, None)
}
