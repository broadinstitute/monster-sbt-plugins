package org.broadinstitute.monster.sbt.model

import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/** Annotation for a Jade dataset column which should drive type generation. */
sealed trait ColumnType extends EnumEntry with Snakecase {
  /**
    * Convert a fully-qualified Scala class for the base column type into
    * a modified (fully-qualified) Scala class based on the type.
    */
  def modify(scalaType: String): String

  /** Name of the BigQuery mode that the column type will map to. */
  def asBigQuery: String
}

object ColumnType extends Enum[ColumnType] with CirceEnum[ColumnType] {
  override val values: IndexedSeq[ColumnType] = findValues

  /**
    *  Marker for columns which should be part of their table's primary key.
    *
    *  Implies that the column is non-optional, and not an array.
    */
  case object PrimaryKey extends ColumnType {
    override def modify(scalaType: String): String = scalaType
    override val asBigQuery: String = "REQUIRED"
  }

  /** Marker for non-optional columns. */
  case object Required extends ColumnType {
    override def modify(scalaType: String): String = scalaType
    override val asBigQuery: String = "REQUIRED"
  }

  /** Marker for optional columns. */
  case object Optional extends ColumnType {

    override def modify(scalaType: String): String =
      s"_root_.scala.Option[$scalaType]"
    override val asBigQuery: String = "NULLABLE"
  }

  /** Marker for columns which contain arrays. */
  case object Repeated extends ColumnType {

    override def modify(scalaType: String): String =
      s"_root_.scala.Array[$scalaType]"
    override val asBigQuery: String = "REPEATED"
  }
}
