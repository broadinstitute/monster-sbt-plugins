package org.broadinstitute.monster.sbt.model.jadeapi

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait JadePartitionMode extends EnumEntry with Lowercase

object JadePartitionMode
    extends Enum[JadePartitionMode]
    with CirceEnum[JadePartitionMode] {
  override val values = findValues

  case object Date extends JadePartitionMode
  case object Int extends JadePartitionMode
}
