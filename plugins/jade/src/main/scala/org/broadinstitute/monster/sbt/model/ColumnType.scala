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

  /**
    * Get the default value of a field based on the column and data type.
    * If there is no default (the field is required), return None.
    */
  def getDefaultValue(scalaType: String): Option[String]

  /** Name of the BigQuery mode that the column type will map to. */
  def asBigQuery: String

  /** True if the column must be non-null. */
  def isRequired: Boolean
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
    override def getDefaultValue(scalaType: String): Option[String] = None
    override val asBigQuery: String = "REQUIRED"
    override val isRequired: Boolean = true
  }

  /** Marker for non-optional columns. */
  case object Required extends ColumnType {
    override def modify(scalaType: String): String = scalaType
    override def getDefaultValue(scalaType: String): Option[String] = None
    override val asBigQuery: String = "REQUIRED"
    override val isRequired: Boolean = true
  }

  /** Marker for optional columns. */
  case object Optional extends ColumnType {

    override def modify(scalaType: String): String =
      s"_root_.scala.Option[$scalaType]"

    override def getDefaultValue(scalaType: String): Option[String] =
      Some(s"_root_.scala.Option.empty[$scalaType]")

    override val asBigQuery: String = "NULLABLE"
    override val isRequired: Boolean = false
  }

  /** Marker for columns which contain arrays. */
  case object Repeated extends ColumnType {

    override def modify(scalaType: String): String =
      s"_root_.scala.collection.immutable.List[$scalaType]"

    override def getDefaultValue(scalaType: String): Option[String] =
      Some(s"_root_.scala.collection.immutable.List.empty[$scalaType]")

    override val asBigQuery: String = "REPEATED"
    override val isRequired: Boolean = false
  }
}
