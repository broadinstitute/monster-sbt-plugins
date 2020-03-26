package org.broadinstitute.monster.sbt.model

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

/** TODO */
sealed trait PartitionMode

object PartitionMode {
  /** TODO */
  case object IngestDate extends PartitionMode
  implicit val ingestDateDecoder: Decoder[IngestDate.type] = deriveDecoder

  /**
    * TODO
    *
    * @param column
    */
  case class DateFromColumn(column: JadeIdentifier) extends PartitionMode
  implicit val dateFromColumnDecoder: Decoder[DateFromColumn] = deriveDecoder

  /**
    * TODO
    *
    * @param column
    * @param begin
    * @param end
    * @param interval
    */
  case class IntRangeFromColumn(
    column: JadeIdentifier,
    begin: Long,
    end: Long,
    interval: Long
  ) extends PartitionMode
  implicit val intRangeFromColumnDecoder: Decoder[IntRangeFromColumn] = deriveDecoder

  /** TODO */
  implicit val modeDecoder: Decoder[PartitionMode] =
    deriveDecoder(renaming.snakeCase, false, Some("mode"))
}
