package org.broadinstitute.monster.sbt.model

import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/** Column type for tabular data in a Jade dataset. */
sealed trait DataType extends EnumEntry with Snakecase {
  /** Fully-qualified name of the Scala class corresponding to the Jade type. */
  def asScala: String
}

object DataType extends Enum[DataType] with CirceEnum[DataType] {
  override val values: IndexedSeq[DataType] = findValues

  // "Simple" column types.
  // Jade supports raw Bytes too, but it's not clear what Scala
  // type would be best to use in that situation and we haven't
  // needed to use it yet.
  // They also support Numeric types, which are similar to Java
  // BigDecimals, but again we haven't needed it yet.
  case object Boolean extends DataType {
    override val asScala: String = "_root_.scala.Boolean"
  }

  case object Float extends DataType {
    override val asScala: String = "_root_.scala.Double"
  }

  case object Integer extends DataType {
    override val asScala: String = "_root_.scala.Long"
  }

  case object String extends DataType {
    override val asScala: String = "_root_.java.lang.String"
  }

  // Time-related column types.
  // Jade also supports datetime / time types, but we haven't had
  // a need for them yet because it's usually better to use one of
  // these two options.
  case object Date extends DataType {
    override val asScala: String = "_root_.java.time.LocalDate"
  }

  case object Timestamp extends DataType {
    override val asScala: String = "_root_.java.time.OffsetDateTime"
  }

  // Jade-specific column types.
  case object DirRef extends DataType {
    override val asScala: String = "_root_.java.lang.String"
  }

  case object FileRef extends DataType {
    override val asScala: String = "_root_.java.lang.String"
  }
}