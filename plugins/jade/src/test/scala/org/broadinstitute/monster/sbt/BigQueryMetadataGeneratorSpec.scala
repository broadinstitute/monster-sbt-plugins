package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.model._
import org.broadinstitute.monster.sbt.model.bigquery.BigQueryColumn
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BigQueryMetadataGeneratorSpec extends AnyFlatSpec with Matchers {

  private val exampleTable = MonsterTable(
    name = new JadeIdentifier("file"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("sample_id"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey,
        links = Vector(
          Link(
            tableName = new JadeIdentifier("sample"),
            columnName = new JadeIdentifier("id")
          )
        )
      ),
      SimpleColumn(
        name = new JadeIdentifier("file_type"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("data_type"),
        datatype = DataType.String,
        `type` = ColumnType.Required
      ),
      SimpleColumn(
        name = new JadeIdentifier("creation_date"),
        datatype = DataType.Date
      ),
      SimpleColumn(
        name = new JadeIdentifier("size"),
        datatype = DataType.Integer
      )
    ),
    structColumns = Vector(
      StructColumn(
        name = new JadeIdentifier("metrics"),
        structName = new JadeIdentifier("file_metrics"),
        `type` = ColumnType.Required
      ),
      StructColumn(
        name = new JadeIdentifier("comments"),
        structName = new JadeIdentifier("comments"),
        `type` = ColumnType.Repeated
      )
    )
  )

  behavior of "BigQueryMetadataGenerator"

  it should "translate table definitions to BQ schemas" in {
    BigQueryMetadataGenerator.tableSchema(exampleTable, Nil) should contain allOf (
      BigQueryColumn("sample_id", "STRING", "REQUIRED"),
      BigQueryColumn("file_type", "STRING", "REQUIRED"),
      BigQueryColumn("data_type", "STRING", "REQUIRED"),
      BigQueryColumn("creation_date", "DATE", "NULLABLE"),
      BigQueryColumn("size", "INT64", "NULLABLE"),
      BigQueryColumn("metrics", "STRING", "REQUIRED"),
      BigQueryColumn("comments", "STRING", "REPEATED")
    )
  }

  it should "extract primary-key column names from table definitions" in {
    BigQueryMetadataGenerator.primaryKeyColumns(exampleTable, Nil) should
      contain allOf ("sample_id", "file_type")
  }

  it should "extract non-primary-key column names from table definitions" in {
    BigQueryMetadataGenerator.nonPrimaryKeyColumns(exampleTable, Nil) should
      contain allOf ("data_type", "creation_date", "size", "metrics", "comments")
  }

  val fragment = MonsterTableFragment(
    name = new JadeIdentifier("f"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("x"),
        datatype = DataType.Timestamp,
        `type` = ColumnType.PrimaryKey
      )
    ),
    structColumns = Vector(
      StructColumn(
        name = new JadeIdentifier("y"),
        structName = new JadeIdentifier("s"),
        `type` = ColumnType.Repeated
      )
    )
  )

  val fragmentTable = new MonsterTable(
    name = new JadeIdentifier("t"),
    tableFragments = Vector(fragment.name),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("z"),
        datatype = DataType.Integer,
        `type` = ColumnType.PrimaryKey
      )
    ),
    structColumns = Vector(
      StructColumn(
        name = new JadeIdentifier("w"),
        structName = new JadeIdentifier("q")
      )
    )
  )

  it should "flatten table fragments when building BQ schemas" in {
    BigQueryMetadataGenerator.tableSchema(fragmentTable, List(fragment)) should contain allOf (
      BigQueryColumn("x", "TIMESTAMP", "REQUIRED"),
      BigQueryColumn("y", "STRING", "REPEATED"),
      BigQueryColumn("z", "INT64", "REQUIRED"),
      BigQueryColumn("w", "STRING", "NULLABLE")
    )
  }

  it should "include fragment PKs when extracting names" in {
    BigQueryMetadataGenerator.primaryKeyColumns(fragmentTable, List(fragment)) should
      contain allOf ("x", "z")
  }

  it should "include fragment non-PK columns when extracting names" in {
    BigQueryMetadataGenerator.nonPrimaryKeyColumns(fragmentTable, List(fragment)) should
      contain allOf ("y", "w")
  }
}
