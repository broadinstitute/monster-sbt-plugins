package org.broadinstitute.monster.sbt.model

import io.circe.jawn.JawnParser
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PartitionModeSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "PartitionMode"

  private val parser = new JawnParser()

  it should "deserialize ingest_date modes" in {
    val modeJson = """{ "mode": "ingest_date" }"""
    parser.decode[PartitionMode](modeJson).right.value shouldBe PartitionMode.IngestDate
  }

  it should "deserialize date_from_column modes" in {
    val modeJson = """{ "mode": "date_from_column", "column": "foo" }"""
    parser.decode[PartitionMode](modeJson).right.value shouldBe
      PartitionMode.DateFromColumn(new JadeIdentifier("foo"))
  }

  it should "deserialize int_range_from_column modes" in {
    val modeJson =
      """{ "mode": "int_range_from_column", "column": "bar", "min": 0, "max": 2, "size": 1 }"""
    parser.decode[PartitionMode](modeJson).right.value shouldBe
      PartitionMode.IntRangeFromColumn(new JadeIdentifier("bar"), 0L, 2L, 1L)
  }
}
