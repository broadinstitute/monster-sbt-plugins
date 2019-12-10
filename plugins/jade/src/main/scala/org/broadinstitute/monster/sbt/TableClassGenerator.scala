package org.broadinstitute.monster.sbt

import io.circe.jawn.JawnParser
import org.broadinstitute.monster.sbt.model.{MonsterColumn, JadeIdentifier, MonsterTable}

/** Utilities for generating Scala classes from Monster table definitions. */
object TableClassGenerator {
  private val parser = new JawnParser()

  /** Scala keywords which must be escaped if used as a column / table name. */
  private val keywords = Set("type")

  /**
    * Generate a Scala case class corresponding to a Jade table.
    *
    * @param tablePackage package which should contain the class
    * @param tableContent JSON content of the Jade table file
    */
  def generateTableClass(
    tablePackage: String,
    tableContent: String
  ): Either[Throwable, String] =
    parser.decode[MonsterTable](tableContent).map { baseTable =>
      val name = className(baseTable)
      val columnFields = baseTable.columns.map(fieldForColumn)

      s"""package $tablePackage
         |
         |case class $name(${columnFields.map(f => s"\n$f").mkString(",")})
         |""".stripMargin
    }

  /** Get the Scala class name for a Jade table. */
  private def className(table: MonsterTable): String =
    snakeToCamel(table.name, titleCase = true)

  /** Get the Scala field declaration for a Jade column. */
  private def fieldForColumn(jadeColumn: MonsterColumn): String = {
    val rawColumnName = snakeToCamel(jadeColumn.name, titleCase = false)
    val columnName =
      if (keywords.contains(rawColumnName)) s"`$rawColumnName`" else rawColumnName

    val columnType = jadeColumn.datatype.asScala
    val modifiedType = jadeColumn.`type`.modify(columnType)

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
