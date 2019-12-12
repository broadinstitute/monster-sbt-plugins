package org.broadinstitute.monster.sbt.model

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JadeIdentifierSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "JadeIdentifier"

  it should "reject empty strings" in {
    val attempt = JadeIdentifier.fromString("")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject capital letters at the beginning of words" in {
    val attempt = JadeIdentifier.fromString("TableName")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject capital letters in the middle of words" in {
    val attempt = JadeIdentifier.fromString("columnName")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject capital letters at the end of words" in {
    val attempt = JadeIdentifier.fromString("columnA")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject kebab-case strings" in {
    val attempt = JadeIdentifier.fromString("the-column")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject leading numbers" in {
    val attempt = JadeIdentifier.fromString("000column")
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "reject identifiers that are 'too long'" in {
    val attempt = JadeIdentifier.fromString("a" * 100)
    attempt.left.value should include("not a valid Jade identifier")
  }

  it should "accept single characters" in {
    val attempt = JadeIdentifier.fromString("c")
    attempt.right.value.id shouldBe "c"
  }

  it should "accept valid lowercase words" in {
    val attempt = JadeIdentifier.fromString("table")
    attempt.right.value.id shouldBe "table"
  }

  it should "accept valid snake_case strings" in {
    val attempt = JadeIdentifier.fromString("snake_case_column")
    attempt.right.value.id shouldBe "snake_case_column"
  }

  it should "accept numbers after the first character" in {
    val attempt = JadeIdentifier.fromString("column123")
    attempt.right.value.id shouldBe "column123"
  }
}
