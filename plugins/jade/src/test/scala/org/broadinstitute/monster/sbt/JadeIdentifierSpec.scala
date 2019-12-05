package org.broadinstitute.monster.sbt

import io.circe.jawn.JawnParser
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JadeIdentifierSpec extends AnyFlatSpec with Matchers with EitherValues {
  private val parser = new JawnParser()

  behavior of "JadeIdentifier"

  it should "reject empty strings" in {
    val attempt = parser.decode[JadeIdentifier]("\"\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "reject capital letters at the beginning of words" in {
    val attempt = parser.decode[JadeIdentifier]("\"TableName\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "reject capital letters in the middle of words" in {
    val attempt = parser.decode[JadeIdentifier]("\"columnName\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "reject capital letters at the end of words" in {
    val attempt = parser.decode[JadeIdentifier]("\"columnA\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "reject kebab-case strings" in {
    val attempt = parser.decode[JadeIdentifier]("\"the-column\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "reject leading numbers" in {
    val attempt = parser.decode[JadeIdentifier]("\"000column\"")
    attempt.left.value.getMessage should include("not a valid Jade identifier")
  }

  it should "accept single characters" in {
    val attempt = parser.decode[JadeIdentifier]("\"c\"")
    attempt.right.value.id shouldBe "c"
  }

  it should "accept valid lowercase words" in {
    val attempt = parser.decode[JadeIdentifier]("\"table\"")
    attempt.right.value.id shouldBe "table"
  }

  it should "accept valid snake_case strings" in {
    val attempt = parser.decode[JadeIdentifier]("\"snake_case_column\"")
    attempt.right.value.id shouldBe "snake_case_column"
  }

  it should "accept numbers after the first character" in {
    val attempt = parser.decode[JadeIdentifier]("\"column123\"")
    attempt.right.value.id shouldBe "column123"
  }
}
