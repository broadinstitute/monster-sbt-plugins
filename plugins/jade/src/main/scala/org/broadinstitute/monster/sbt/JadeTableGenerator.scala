package org.broadinstitute.monster.sbt

import io.circe.jawn.JawnParser

object JadeTableGenerator {
  private val parser = new JawnParser()

  /** TODO */
  def generateTableClass(
    tablePackage: String,
    tableContent: String
  ): Either[Throwable, String] =
    parser.decode[JadeTable](tableContent).map { baseTable =>
      val name = className(baseTable)
      val columnFields = baseTable.columns.map(fieldForColumn)

      s"""package $tablePackage
         |
         |case class $name(${columnFields.map(f => s"\n$f").mkString(",")})
         |""".stripMargin
    }

  /** TODO */
  private def className(table: JadeTable): String =
    snakeToCamel(table.name, titleCase = true)

  /** TODO */
  private def fieldForColumn(jadeColumn: JadeColumn): String = {
    val columnName = snakeToCamel(jadeColumn.name, titleCase = false)
    val columnType = jadeColumn.datatype.asScala
    val modifiedType = jadeColumn.modifier.modify(columnType)

    s"$columnName: $modifiedType"
  }

  /** TODO */
  private def snakeToCamel(id: JadeIdentifier, titleCase: Boolean): String = {
    val res = id.id.split("_", 0).map(x => x(0).toUpper + x.drop(1)).mkString
    if (titleCase) {
      res
    } else {
      res(0).toLower + res.drop(1)
    }
  }
}
