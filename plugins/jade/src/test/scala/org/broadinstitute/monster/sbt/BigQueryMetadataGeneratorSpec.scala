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
    BigQueryMetadataGenerator.tableSchema(exampleTable) should contain allOf (
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
    BigQueryMetadataGenerator.primaryKeyColumns(exampleTable) should
      contain allOf ("sample_id", "file_type")
  }

  it should "extract non-primary-key column names from table definitions" in {
    BigQueryMetadataGenerator.nonPrimaryKeyColumns(exampleTable) should
      contain allOf ("data_type", "creation_date", "size", "metrics", "comments")
  }
}
