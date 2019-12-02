package org.broadinstitute.monster.sbt

import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/** TODO */
sealed trait JadeColumnModifier extends EnumEntry with Snakecase {
  /** TODO */
  def modify(scalaType: String): String
}

object JadeColumnModifier
    extends Enum[JadeColumnModifier]
    with CirceEnum[JadeColumnModifier] {
  override val values: IndexedSeq[JadeColumnModifier] = findValues

  /** TODO */
  case object Normal extends JadeColumnModifier {

    override def modify(scalaType: String): String =
      s"_root_.scala.Option[$scalaType]"
  }

  /** TODO */
  case object Array extends JadeColumnModifier {

    override def modify(scalaType: String): String =
      s"_root_.scala.Array[$scalaType]"
  }

  /** TODO */
  case object PrimaryKey extends JadeColumnModifier {
    override def modify(scalaType: String): String = scalaType
  }
}
