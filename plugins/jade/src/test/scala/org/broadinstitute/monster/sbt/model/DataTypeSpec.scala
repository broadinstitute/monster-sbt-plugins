package org.broadinstitute.monster.sbt.model

import io.circe.Json
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataTypeSpec extends AnyFlatSpec with Matchers {
  behavior of "DataType"

  it should "serialize fileref columns without an underscore" in {
    (DataType.FileRef: DataType).asJson shouldBe Json.fromString("fileref")
  }

  it should "serialize dirref columns without an underscore" in {
    (DataType.DirRef: DataType).asJson shouldBe Json.fromString("dirref")
  }
}
