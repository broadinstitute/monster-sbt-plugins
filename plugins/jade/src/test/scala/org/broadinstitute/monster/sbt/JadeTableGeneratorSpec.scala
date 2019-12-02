package org.broadinstitute.monster.sbt

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JadeTableGeneratorSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "JadeTableGenerator"

  private val testPackage = "foo.bar"

  def checkGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = JadeTableGenerator.generateTableClass(testPackage, input)
      out.right.value shouldBe output
    }

  def checkFailedGeneration(description: String, input: String, error: String): Unit =
    it should description in {
      val out = JadeTableGenerator.generateTableClass(testPackage, input)
      out.left.value.getMessage should include(error)
    }

  // Happy cases
  it should behave like checkGeneration(
    "generate a zero-column table",
    s"""{
       |  "name": "no_columns",
       |  "columns": []
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class NoColumns()
       |""".stripMargin
  )
  it should behave like checkGeneration(
    "generate a one-column table",
    s"""{
       |  "name": "one_column",
       |  "columns": [
       |    {
       |      "name": "test_column",
       |      "datatype": "string"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class OneColumn(
       |testColumn: _root_.scala.Option[_root_.java.lang.String])
       |""".stripMargin
  )
  it should behave like checkGeneration(
    "generate every type of column",
    s"""{
       |  "name": "all_columns",
       |  "columns": [
       |    {
       |      "name": "bool_column",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "float_column",
       |      "datatype": "float"
       |    },
       |    {
       |      "name": "int_column",
       |      "datatype": "integer"
       |    },
       |    {
       |      "name": "string_column",
       |      "datatype": "string"
       |    },
       |    {
       |      "name": "date_column",
       |      "datatype": "date"
       |    },
       |    {
       |      "name": "timestamp_column",
       |      "datatype": "timestamp"
       |    },
       |    {
       |      "name": "dir_column",
       |      "datatype": "dir_ref"
       |    },
       |    {
       |      "name": "file_column",
       |      "datatype": "file_ref"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class AllColumns(
       |boolColumn: _root_.scala.Option[_root_.scala.Boolean],
       |floatColumn: _root_.scala.Option[_root_.scala.Double],
       |intColumn: _root_.scala.Option[_root_.scala.Long],
       |stringColumn: _root_.scala.Option[_root_.java.lang.String],
       |dateColumn: _root_.scala.Option[_root_.java.time.LocalDate],
       |timestampColumn: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       |dirColumn: _root_.scala.Option[_root_.java.lang.String],
       |fileColumn: _root_.scala.Option[_root_.java.lang.String])
       |""".stripMargin
  )
  // NOTE: This is copy-pasted from the expected output of the test above.
  // Scalatest's compilation-checking helpers only work on string literals (sigh),
  // so we can't run the check automatically as part of the existing helper function.
  it should "output compile-able code with all types" in {
    """case class AllColumns(
       boolColumn: _root_.scala.Option[_root_.scala.Boolean],
       floatColumn: _root_.scala.Option[_root_.scala.Double],
       intColumn: _root_.scala.Option[_root_.scala.Long],
       stringColumn: _root_.scala.Option[_root_.java.lang.String],
       dateColumn: _root_.scala.Option[_root_.java.time.LocalDate],
       timestampColumn: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       dirColumn: _root_.scala.Option[_root_.java.lang.String],
       fileColumn: _root_.scala.Option[_root_.java.lang.String])
       """ should compile
  }
  it should behave like checkGeneration(
    "generate primary key columns",
    s"""{
       |  "name": "key_column",
       |  "columns": [
       |    {
       |      "name": "test_key",
       |      "datatype": "string",
       |      "modifier": "primary_key"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class KeyColumn(
       |testKey: _root_.java.lang.String)
       |""".stripMargin
  )
  it should behave like checkGeneration(
    "generate array columns",
    s"""{
       |  "name": "array_column",
       |  "columns": [
       |    {
       |      "name": "test_array",
       |      "datatype": "float",
       |      "modifier": "array"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class ArrayColumn(
       |testArray: _root_.scala.Array[_root_.scala.Double])
       |""".stripMargin
  )
  it should behave like checkGeneration(
    "mix all kinds of column modifiers",
    s"""{
       |  "name": "all_modifiers",
       |  "columns": [
       |    {
       |      "name": "key_column",
       |      "datatype": "date",
       |      "modifier": "primary_key"
       |    },
       |    {
       |      "name": "normal_column",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "array_column",
       |      "datatype": "integer",
       |      "modifier": "array"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class AllModifiers(
       |keyColumn: _root_.java.time.LocalDate,
       |normalColumn: _root_.scala.Option[_root_.scala.Boolean],
       |arrayColumn: _root_.scala.Array[_root_.scala.Long])
       |""".stripMargin
  )
  // NOTE: This is copy-pasted from the expected output of the test above.
  // Scalatest's compilation-checking helpers only work on string literals (sigh),
  // so we can't run the check automatically as part of the existing helper function.
  it should "output compile-able code with all modifiers" in {
    """case class AllModifiers(
       keyColumn: _root_.java.time.LocalDate,
       normalColumn: _root_.scala.Option[_root_.scala.Boolean],
       arrayColumn: _root_.scala.Array[_root_.scala.Long])
       """ should compile
  }

  // Sad cases
  it should behave like checkFailedGeneration(
    "catch invalid table payloads",
    """{ "id": "foobar", "nodes": [] }""",
    "failed cursor"
  )
  it should behave like checkFailedGeneration(
    "catch invalid table identifiers",
    """{ "name": "tableOne", "columns": [] }""",
    "Illegal character 'O'"
  )
  it should behave like checkFailedGeneration(
    "catch invalid column identifiers",
    """{ "name": "ok_table", "columns": [{ "name": "bad-column", "datatype": "string" }] }""",
    "Illegal character '-'"
  )
}
