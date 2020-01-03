package org.broadinstitute.monster.sbt

import io.circe.jawn.JawnParser
import org.broadinstitute.monster.sbt.model.{
  JadeIdentifier,
  MonsterTable,
  SimpleColumn,
  Struct,
  StructColumn
}

/** Utilities for generating Scala classes from Monster table definitions. */
object ClassGenerator {
  private val parser = new JawnParser()

  /** Scala keywords which must be escaped if used as a column / table name. */
  private val keywords = Set("type")

  /**
    * Generate a Scala case class corresponding to a Jade table.
    *
    * @param tablePackage package which should contain the class
    * @param structPackage package where any structs referenced by the table
    *                      should be located
    * @param tableContent JSON content of the Jade table file
    */
  def generateTableClass(
    tablePackage: String,
    structPackage: String,
    tableContent: String
  ): Either[Throwable, String] =
    parser.decode[MonsterTable](tableContent).map { baseTable =>
      val name = snakeToCamel(baseTable.name, titleCase = true)
      val simpleFields = baseTable.columns.map(fieldForColumn)
      val structFields = baseTable.structColumns.map(fieldForStruct(structPackage, _))
      val classParams = (simpleFields ++ structFields).map(f => s"\n$f").mkString(",")

      s"""package $tablePackage
         |
         |case class $name($classParams)
         |""".stripMargin
    }

  /**
    * Generate a Scala case class corresponding to a nested struct.
    *
    * @param structPackage package which should contain the class
    * @param structContent JSON content of the Jade struct file
    */
  def generateStructClass(
    structPackage: String,
    structContent: String
  ): Either[Throwable, String] =
    parser.decode[Struct](structContent).map { baseStruct =>
      val name = snakeToCamel(baseStruct.name, titleCase = true)
      val fields = baseStruct.fields.map(fieldForColumn)
      val classParams = fields.map(f => s"\n$f").mkString(",")

      s"""package $structPackage
         |
         |case class $name($classParams)
         |""".stripMargin
    }

  /** Get the Scala field declaration for a Jade column. */
  private def fieldForColumn(jadeColumn: SimpleColumn): String = {
    val rawColumnName = snakeToCamel(jadeColumn.name, titleCase = false)
    val columnName =
      if (keywords.contains(rawColumnName)) s"`$rawColumnName`" else rawColumnName

    val columnType = jadeColumn.datatype.asScala
    val modifiedType = jadeColumn.`type`.modify(columnType)

    s"$columnName: $modifiedType"
  }

  /** Get the Scala field declaration for a Jade column which references a struct. */
  private def fieldForStruct(
    structPackage: String,
    structColumn: StructColumn
  ): String = {
    val rawColumnName = snakeToCamel(structColumn.name, titleCase = false)
    val columnName =
      if (keywords.contains(rawColumnName)) s"`$rawColumnName`" else rawColumnName

    val structType =
      s"_root_.$structPackage.${snakeToCamel(structColumn.structName, titleCase = true)}"
    val modifiedType = structColumn.`type`.modify(structType)

    s"$columnName: $modifiedType"
  }

  /**
    * Convert a snake_case Jade identifier to Scala camelCase.
    *
    * Jade IDs are enforced to be snake_case on deserialization.
    *
    * @param titleCase if true, the first character of the output will be capitalized
    */
  private def snakeToCamel(id: JadeIdentifier, titleCase: Boolean): String = {
    val res = id.id.split("_", 0).map(x => x(0).toUpper + x.drop(1)).mkString
    if (titleCase) {
      res
    } else {
      res(0).toLower + res.drop(1)
    }
  }
}
