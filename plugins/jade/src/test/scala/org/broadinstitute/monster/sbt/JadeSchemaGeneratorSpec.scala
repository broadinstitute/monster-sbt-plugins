package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.model._
import org.broadinstitute.monster.sbt.model.jadeapi._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JadeSchemaGeneratorSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "JadeSchemaGenerator"

  private val participants = MonsterTable(
    name = new JadeIdentifier("participant"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("id"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("age"),
        datatype = DataType.Integer
      )
    ),
    structColumns = Vector(
      StructColumn(
        name = new JadeIdentifier("attributes"),
        structName = new JadeIdentifier("donor_attributes")
      )
    ),
    partitioning = PartitionMode.IntRangeFromColumn(
      column = new JadeIdentifier("age"),
      min = 0L,
      max = 120L,
      size = 1L
    )
  )

  private val samples = MonsterTable(
    name = new JadeIdentifier("sample"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("id"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("participant_id"),
        datatype = DataType.String,
        links = Vector(
          Link(
            tableName = participants.name,
            columnName = new JadeIdentifier("id")
          )
        )
      ),
      SimpleColumn(
        name = new JadeIdentifier("sample_times"),
        datatype = DataType.Timestamp,
        `type` = ColumnType.Repeated
      )
    )
  )

  private val files = MonsterTable(
    name = new JadeIdentifier("file"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("sample_id"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey,
        links = Vector(
          Link(
            tableName = samples.name,
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
        name = new JadeIdentifier("path"),
        datatype = DataType.FileRef
      ),
      SimpleColumn(
        name = new JadeIdentifier("creation_date"),
        datatype = DataType.Date
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
    ),
    partitioning = PartitionMode.DateFromColumn(new JadeIdentifier("creation_date"))
  )

  it should "build a Jade schema from a collection of Monster tables" in {
    val sourceTables = List(participants, samples, files)

    val expected = JadeSchema(
      tables = Set(
        JadeTable(
          name = participants.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("age"),
              datatype = DataType.Integer,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("attributes"),
              datatype = DataType.String,
              arrayOf = false
            )
          ),
          primaryKey = Set(new JadeIdentifier("id")),
          partitionMode = JadePartitionMode.Int,
          datePartitionOptions = None,
          intPartitionOptions = Some {
            JadeIntPartitionOptions(
              column = new JadeIdentifier("age"),
              min = 0L,
              max = 120L,
              interval = 1L
            )
          }
        ),
        JadeTable(
          name = samples.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("participant_id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("sample_times"),
              datatype = DataType.Timestamp,
              arrayOf = true
            )
          ),
          primaryKey = Set(new JadeIdentifier("id")),
          partitionMode = JadePartitionMode.Date,
          datePartitionOptions = Some(JadeDatePartitionOptions.IngestDate),
          intPartitionOptions = None
        ),
        JadeTable(
          name = files.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("sample_id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("file_type"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("data_type"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("metrics"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("comments"),
              datatype = DataType.String,
              arrayOf = true
            ),
            JadeColumn(
              name = new JadeIdentifier("path"),
              datatype = DataType.FileRef,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("creation_date"),
              datatype = DataType.Date,
              arrayOf = false
            )
          ),
          primaryKey = Set(new JadeIdentifier("sample_id"), new JadeIdentifier("file_type")),
          partitionMode = JadePartitionMode.Date,
          datePartitionOptions =
            Some(JadeDatePartitionOptions(new JadeIdentifier("creation_date"))),
          intPartitionOptions = None
        )
      ),
      relationships = Set(
        JadeRelationship(
          from = JadeRelationshipRef(
            table = samples.name,
            column = new JadeIdentifier("participant_id")
          ),
          to = JadeRelationshipRef(
            table = participants.name,
            column = new JadeIdentifier("id")
          )
        ),
        JadeRelationship(
          from = JadeRelationshipRef(
            table = files.name,
            column = new JadeIdentifier("sample_id")
          ),
          to = JadeRelationshipRef(
            table = samples.name,
            column = new JadeIdentifier("id")
          )
        )
      )
    )

    JadeSchemaGenerator
      .generateSchema(sourceTables, Nil)
      .right
      .value shouldBe expected
  }

  it should "flatten table fragments when building Jade schemas" in {
    val fragment = MonsterTableFragment(
      name = new JadeIdentifier("shared"),
      columns = Vector(
        SimpleColumn(
          name = new JadeIdentifier("internal_id"),
          datatype = DataType.Timestamp,
          `type` = ColumnType.PrimaryKey
        ),
        SimpleColumn(
          name = new JadeIdentifier("audit_flags"),
          datatype = DataType.String,
          `type` = ColumnType.Repeated
        )
      )
    )

    val schema = JadeSchemaGenerator.generateSchema(
      List(participants.copy(tableFragments = Vector(fragment.name))),
      List(fragment)
    )

    val expected = JadeSchema(
      tables = Set(
        JadeTable(
          name = participants.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("age"),
              datatype = DataType.Integer,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("attributes"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("internal_id"),
              datatype = DataType.Timestamp,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("audit_flags"),
              datatype = DataType.String,
              arrayOf = true
            )
          ),
          primaryKey = Set(new JadeIdentifier("id"), new JadeIdentifier("internal_id")),
          partitionMode = JadePartitionMode.Int,
          datePartitionOptions = None,
          intPartitionOptions = Some {
            JadeIntPartitionOptions(
              column = new JadeIdentifier("age"),
              min = 0L,
              max = 120L,
              interval = 1L
            )
          }
        )
      ),
      relationships = Set.empty
    )

    schema.right.value shouldBe expected
  }

  it should "support relationships specified in a fragment" in {
    val fragment = MonsterTableFragment(
      name = new JadeIdentifier("shared"),
      columns = Vector(
        SimpleColumn(
          name = new JadeIdentifier("emr_id"),
          datatype = DataType.String,
          links = Vector(
            Link(
              tableName = new JadeIdentifier("emr"),
              columnName = new JadeIdentifier("id")
            )
          )
        )
      )
    )

    val emrs = MonsterTable(
      name = new JadeIdentifier("emr"),
      columns = Vector(
        SimpleColumn(
          name = new JadeIdentifier("id"),
          datatype = DataType.String,
          `type` = ColumnType.PrimaryKey
        )
      )
    )

    val schema = JadeSchemaGenerator.generateSchema(
      List(participants.copy(tableFragments = Vector(fragment.name)), emrs),
      List(fragment)
    )

    val expected = JadeSchema(
      tables = Set(
        JadeTable(
          name = participants.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("id"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("age"),
              datatype = DataType.Integer,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("attributes"),
              datatype = DataType.String,
              arrayOf = false
            ),
            JadeColumn(
              name = new JadeIdentifier("emr_id"),
              datatype = DataType.String,
              arrayOf = false
            )
          ),
          primaryKey = Set(new JadeIdentifier("id")),
          partitionMode = JadePartitionMode.Int,
          datePartitionOptions = None,
          intPartitionOptions = Some {
            JadeIntPartitionOptions(
              column = new JadeIdentifier("age"),
              min = 0L,
              max = 120L,
              interval = 1L
            )
          }
        ),
        JadeTable(
          name = emrs.name,
          columns = Set(
            JadeColumn(
              name = new JadeIdentifier("id"),
              datatype = DataType.String,
              arrayOf = false
            )
          ),
          primaryKey = Set(new JadeIdentifier("id")),
          partitionMode = JadePartitionMode.Date,
          datePartitionOptions = Some(JadeDatePartitionOptions.IngestDate),
          intPartitionOptions = None
        )
      ),
      relationships = Set(
        JadeRelationship(
          from = JadeRelationshipRef(
            table = participants.name,
            column = new JadeIdentifier("emr_id")
          ),
          to = JadeRelationshipRef(
            table = emrs.name,
            column = new JadeIdentifier("id")
          )
        )
      )
    )

    schema.right.value shouldBe expected
  }

  it should "fail to build schemas with invalid relationships" in {
    // Missing the samples table referred to by the files table.
    val sourceTables = List(participants, files)

    val err = JadeSchemaGenerator
      .generateSchema(sourceTables, Nil)
      .left
      .value

    err should include(samples.name.id)
    err should include("id")
  }

  it should "fail to build schemas with invalid fragment references" in {
    val sourceTables = List(participants.copy(tableFragments = Vector(new JadeIdentifier("fake"))))
    val err = JadeSchemaGenerator.generateSchema(sourceTables, Nil).left.value

    err should include("fake")
  }
}
