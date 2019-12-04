package org.broadinstitute.monster.sbt

import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/** Annotation for a Jade dataset column which should drive type generation. */
sealed trait JadeColumnModifier extends EnumEntry with Snakecase {
  /**
    * Convert a fully-qualified Scala class for the base column type into
    * a modified (fully-qualified) Scala class based on the annotation.
    */
  def modify(scalaType: String): String
}

object JadeColumnModifier
    extends Enum[JadeColumnModifier]
    with CirceEnum[JadeColumnModifier] {
  override val values: IndexedSeq[JadeColumnModifier] = findValues

  /**
    * Modifier for "normal" data columns.
    *
    * Marks columns as optional.
    */
  case object Normal extends JadeColumnModifier {

    override def modify(scalaType: String): String =
      s"_root_.scala.Option[$scalaType]"
  }

  /**
    * Modifier for repeated data columns.
    *
    * Marks columns as arrays.
    */
  case object Array extends JadeColumnModifier {

    override def modify(scalaType: String): String =
      s"_root_.scala.Array[$scalaType]"
  }

  /**
    * Modifier for primary-key columns.
    *
    * Does not modify the base type.
    */
  case object PrimaryKey extends JadeColumnModifier {
    override def modify(scalaType: String): String = scalaType
  }
}
