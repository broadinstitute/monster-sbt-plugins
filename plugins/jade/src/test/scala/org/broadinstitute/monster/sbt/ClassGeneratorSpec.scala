package org.broadinstitute.monster.sbt

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ClassGeneratorSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "ClassGenerator"

  private val testPackage = "foo.bar"
  private val structPackage = testPackage.reverse

  def checkTableGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = ClassGenerator.generateTableClass(testPackage, structPackage, input)
      out.right.value shouldBe output
    }

  def checkFailedTableGeneration(
    description: String,
    input: String,
    error: String
  ): Unit = it should description in {
    val out = ClassGenerator.generateTableClass(testPackage, structPackage, input)
    out.left.value.getMessage should include(error)
  }

  def checkStructGeneration(description: String, input: String, output: String): Unit =
    it should description in {
      val out = ClassGenerator.generateStructClass(structPackage, input)
      out.right.value shouldBe output
    }

  def checkFailedStructGeneration(
    description: String,
    input: String,
    error: String
  ): Unit = it should description in {
    val out = ClassGenerator.generateStructClass(structPackage, input)
    out.left.value.getMessage should include(error)
  }

  // Happy table cases
  it should behave like checkTableGeneration(
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
  it should behave like checkTableGeneration(
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
  it should behave like checkTableGeneration(
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
  it should behave like checkTableGeneration(
    "generate required columns",
    s"""{
       |  "name": "required_column",
       |  "columns": [
       |    {
       |      "name": "test_required",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RequiredColumn(
       |testRequired: _root_.java.lang.String)
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate primary key columns",
    s"""{
       |  "name": "key_column",
       |  "columns": [
       |    {
       |      "name": "test_key",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class KeyColumn(
       |testKey: _root_.java.lang.String)
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "generate array columns",
    s"""{
       |  "name": "array_column",
       |  "columns": [
       |    {
       |      "name": "test_array",
       |      "datatype": "float",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class ArrayColumn(
       |testArray: _root_.scala.Array[_root_.scala.Double])
       |""".stripMargin
  )
  it should behave like checkTableGeneration(
    "mix all kinds of column types",
    s"""{
       |  "name": "all_modifiers",
       |  "columns": [
       |    {
       |      "name": "key_column",
       |      "datatype": "date",
       |      "type": "primary_key"
       |    },
       |    {
       |      "name": "normal_column",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "array_column",
       |      "datatype": "integer",
       |      "type": "repeated"
       |    },
       |    {
       |      "name": "required_column",
       |      "datatype": "float",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class AllModifiers(
       |keyColumn: _root_.java.time.LocalDate,
       |normalColumn: _root_.scala.Option[_root_.scala.Boolean],
       |arrayColumn: _root_.scala.Array[_root_.scala.Long],
       |requiredColumn: _root_.scala.Double)
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

  it should behave like checkTableGeneration(
    "escape columns named `type` in generated code",
    s"""{
       |  "name": "type_column",
       |  "columns": [
       |    {
       |      "name": "type",
       |      "datatype": "float"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class TypeColumn(
       |`type`: _root_.scala.Option[_root_.scala.Double])
       |""".stripMargin
  )
  it should "output compile-able code with escaped fields" in {
    """case class TypeColumn(
       `type`: _root_.scala.Option[_root_.scala.Double])
       """ should compile
  }

  it should behave like checkTableGeneration(
    "generate references to struct classes",
    s"""{
       |  "name": "struct_column",
       |  "struct_columns": [
       |    {
       |      "name": "comment",
       |      "struct_name": "row_comment"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class StructColumn(
       |comment: _root_.scala.Option[_root_.$structPackage.RowComment])
       |""".stripMargin
  )

  it should behave like checkTableGeneration(
    "generate required struct references",
    s"""{
       |  "name": "required_struct",
       |  "struct_columns": [
       |    {
       |      "name": "required_comment",
       |      "struct_name": "comment",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RequiredStruct(
       |requiredComment: _root_.$structPackage.Comment)
       |""".stripMargin
  )

  it should behave like checkTableGeneration(
    "generate repeated struct references",
    s"""{
       |  "name": "repeated_struct",
       |  "struct_columns": [
       |    {
       |      "name": "repeated_comment",
       |      "struct_name": "comment123",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $testPackage
       |
       |case class RepeatedStruct(
       |repeatedComment: _root_.scala.Array[_root_.$structPackage.Comment123])
       |""".stripMargin
  )

  // Sad table cases
  it should behave like checkFailedTableGeneration(
    "catch invalid table payloads",
    """{ "id": "foobar", "nodes": [] }""",
    "failed cursor"
  )
  it should behave like checkFailedTableGeneration(
    "catch invalid table identifiers",
    """{ "name": "tableOne", "columns": [] }""",
    "not a valid Jade identifier"
  )
  it should behave like checkFailedTableGeneration(
    "catch invalid column identifiers",
    """{ "name": "ok_table", "columns": [{ "name": "bad-column", "datatype": "string" }] }""",
    "not a valid Jade identifier"
  )

  // Happy struct cases
  it should behave like checkStructGeneration(
    "generate a zero-field struct",
    s"""{
       |  "name": "no_fields",
       |  "fields": []
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class NoFields()
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate a one-field struct",
    s"""{
       |  "name": "one_field",
       |  "fields": [
       |    {
       |      "name": "test_field",
       |      "datatype": "string"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class OneField(
       |testField: _root_.scala.Option[_root_.java.lang.String])
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate every type of field",
    s"""{
       |  "name": "all_fields",
       |  "fields": [
       |    {
       |      "name": "bool_field",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "float_field",
       |      "datatype": "float"
       |    },
       |    {
       |      "name": "int_field",
       |      "datatype": "integer"
       |    },
       |    {
       |      "name": "string_field",
       |      "datatype": "string"
       |    },
       |    {
       |      "name": "date_field",
       |      "datatype": "date"
       |    },
       |    {
       |      "name": "timestamp_field",
       |      "datatype": "timestamp"
       |    },
       |    {
       |      "name": "dir_field",
       |      "datatype": "dir_ref"
       |    },
       |    {
       |      "name": "file_field",
       |      "datatype": "file_ref"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class AllFields(
       |boolField: _root_.scala.Option[_root_.scala.Boolean],
       |floatField: _root_.scala.Option[_root_.scala.Double],
       |intField: _root_.scala.Option[_root_.scala.Long],
       |stringField: _root_.scala.Option[_root_.java.lang.String],
       |dateField: _root_.scala.Option[_root_.java.time.LocalDate],
       |timestampField: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       |dirField: _root_.scala.Option[_root_.java.lang.String],
       |fileField: _root_.scala.Option[_root_.java.lang.String])
       |""".stripMargin
  )
  it should "output compile-able struct code with all types" in {
    """case class AllFields(
       boolField: _root_.scala.Option[_root_.scala.Boolean],
       floatField: _root_.scala.Option[_root_.scala.Double],
       intField: _root_.scala.Option[_root_.scala.Long],
       stringField: _root_.scala.Option[_root_.java.lang.String],
       dateField: _root_.scala.Option[_root_.java.time.LocalDate],
       timestampField: _root_.scala.Option[_root_.java.time.OffsetDateTime],
       dirField: _root_.scala.Option[_root_.java.lang.String],
       fileField: _root_.scala.Option[_root_.java.lang.String])
       """ should compile
  }
  it should behave like checkStructGeneration(
    "generate required fields",
    s"""{
       |  "name": "required_field",
       |  "fields": [
       |    {
       |      "name": "test_required",
       |      "datatype": "string",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class RequiredField(
       |testRequired: _root_.java.lang.String)
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "generate array fields",
    s"""{
       |  "name": "array_field",
       |  "fields": [
       |    {
       |      "name": "test_array",
       |      "datatype": "float",
       |      "type": "repeated"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class ArrayField(
       |testArray: _root_.scala.Array[_root_.scala.Double])
       |""".stripMargin
  )
  it should behave like checkStructGeneration(
    "mix all kinds of column fields",
    s"""{
       |  "name": "all_modifiers",
       |  "fields": [
       |    {
       |      "name": "key_field",
       |      "datatype": "date",
       |      "type": "primary_key"
       |    },
       |    {
       |      "name": "normal_field",
       |      "datatype": "boolean"
       |    },
       |    {
       |      "name": "array_field",
       |      "datatype": "integer",
       |      "type": "repeated"
       |    },
       |    {
       |      "name": "required_field",
       |      "datatype": "float",
       |      "type": "required"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class AllModifiers(
       |keyField: _root_.java.time.LocalDate,
       |normalField: _root_.scala.Option[_root_.scala.Boolean],
       |arrayField: _root_.scala.Array[_root_.scala.Long],
       |requiredField: _root_.scala.Double)
       |""".stripMargin
  )
  it should "output compile-able struct code with all modifiers" in {
    """case class AllModifiers(
       keyField: _root_.java.time.LocalDate,
       normalField: _root_.scala.Option[_root_.scala.Boolean],
       arrayField: _root_.scala.Array[_root_.scala.Long])
       """ should compile
  }

  it should behave like checkStructGeneration(
    "escape columns named `type` in generated struct code",
    s"""{
       |  "name": "type_field",
       |  "fields": [
       |    {
       |      "name": "type",
       |      "datatype": "float"
       |    }
       |  ]
       |}""".stripMargin,
    s"""package $structPackage
       |
       |case class TypeField(
       |`type`: _root_.scala.Option[_root_.scala.Double])
       |""".stripMargin
  )
  it should "output compile-able struct code with escaped fields" in {
    """case class TypeField(
       `type`: _root_.scala.Option[_root_.scala.Double])
       """ should compile
  }

  // Sad struct cases
  it should behave like checkFailedStructGeneration(
    "catch invalid struct payloads",
    """{ "id": "foobar", "nodes": [] }""",
    "failed cursor"
  )
  it should behave like checkFailedStructGeneration(
    "catch invalid struct identifiers",
    """{ "name": "structOne", "fields": [] }""",
    "not a valid Jade identifier"
  )
  it should behave like checkFailedStructGeneration(
    "catch invalid field identifiers",
    """{ "name": "ok_struct", "fields": [{ "name": "bad-field", "datatype": "string" }] }""",
    "not a valid Jade identifier"
  )
}
