package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/**
  * Specification for how a Jade BigQuery table should be partitioned.
  *
  * Jade defaults to no partitioning for backwards compatibility, but
  * BigQuery recommends that all tables be partitioned by *something*,
  * so we don't expose that option here.
  */
sealed trait PartitionMode

object PartitionMode {
  /**
    * Partition rows by the date they were ingested into BigQuery.
    *
    * Jade's API merges this with the date-from-column partition mode,
    * with a reserved string matching the name of the ingest-date column.
    * We separate it into an entirely different option for max clarity.
    */
  case object IngestDate extends PartitionMode
  implicit val ingestDateDecoder: Decoder[IngestDate.type] = deriveDecoder

  /**
    * Partition rows by the values of a DATE or TIMESTAMP column.
    *
    * @param column name of the column to partition on
    */
  case class DateFromColumn(column: JadeIdentifier) extends PartitionMode
  implicit val dateFromColumnDecoder: Decoder[DateFromColumn] = deriveDecoder

  /**
    * Partition rows by intervals of the values in an INTEGER column
    *
    * @param column name of the column to partition on
    * @param min smallest value to support in partitioning. A row with a value
    *            smaller than this will land in the __UNPARTITIONED__ partition
    * @param max largest value to support in partitioning. A row with a value
    *            larger than this will land in the __UNPARTITIONED__ partition
    * @param size size to use for the ranges that fall into each partition.
    *             (max - min) / size must be <= 40K, or BigQuery will complain
    */
  case class IntRangeFromColumn(column: JadeIdentifier, min: Long, max: Long, size: Long)
      extends PartitionMode
  implicit val intRangeFromColumnDecoder: Decoder[IntRangeFromColumn] = deriveDecoder

  implicit val modeDecoder: Decoder[PartitionMode] =
    // NOTE: Some("mode") here establishes that any JSON object we attempt to
    // decode into a PartitionMode must include a "mode" key, with a value matching
    // the snake-case-ified name of one of the cases above.
    deriveDecoder(renaming.snakeCase, false, Some("mode"))
}
