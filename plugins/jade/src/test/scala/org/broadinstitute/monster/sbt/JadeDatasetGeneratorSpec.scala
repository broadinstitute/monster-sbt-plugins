package org.broadinstitute.monster.sbt

import java.util.UUID

import org.broadinstitute.monster.sbt.model._
import org.broadinstitute.monster.sbt.model.jadeapi._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JadeDatasetGeneratorSpec extends AnyFlatSpec with Matchers with EitherValues {
  behavior of "JadeDatasetGenerator"

  private val participants = MonsterTable(
    name = new JadeIdentifier("participant"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("id"),
        datatype = Datatype.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("age"),
        datatype = Datatype.Integer
      )
    ),
    structColumns = Vector(
      StructColumn(
        name = new JadeIdentifier("attributes"),
        structName = new JadeIdentifier("donor_attributes")
      )
    )
  )

  private val samples = MonsterTable(
    name = new JadeIdentifier("sample"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("id"),
        datatype = Datatype.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("participant_id"),
        datatype = Datatype.String,
        links = Vector(
          Link(
            tableName = participants.name,
            columnName = new JadeIdentifier("id")
          )
        )
      ),
      SimpleColumn(
        name = new JadeIdentifier("sample_times"),
        datatype = Datatype.Timestamp,
        `type` = ColumnType.Repeated
      )
    )
  )

  private val files = MonsterTable(
    name = new JadeIdentifier("file"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("sample_id"),
        datatype = Datatype.String,
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
        datatype = Datatype.String,
        `type` = ColumnType.PrimaryKey
      ),
      SimpleColumn(
        name = new JadeIdentifier("data_type"),
        datatype = Datatype.String,
        `type` = ColumnType.Required
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

  it should "build a Jade dataset from a collection of Monster tables" in {
    val sourceTables = List(participants, samples, files)
    val expected = JadeDataset(
      name = new JadeIdentifier("test_dataset"),
      description = "Test test test",
      defaultProfileId = UUID.randomUUID(),
      schema = JadeSchema(
        tables = Set(
          JadeTable(
            name = participants.name,
            columns = Set(
              JadeColumn(
                name = new JadeIdentifier("id"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("age"),
                datatype = Datatype.Integer,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("attributes"),
                datatype = Datatype.String,
                arrayOf = false
              )
            ),
            primaryKey = Set(new JadeIdentifier("id"))
          ),
          JadeTable(
            name = samples.name,
            columns = Set(
              JadeColumn(
                name = new JadeIdentifier("id"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("participant_id"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("sample_times"),
                datatype = Datatype.Timestamp,
                arrayOf = true
              )
            ),
            primaryKey = Set(new JadeIdentifier("id"))
          ),
          JadeTable(
            name = files.name,
            columns = Set(
              JadeColumn(
                name = new JadeIdentifier("sample_id"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("file_type"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("data_type"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("metrics"),
                datatype = Datatype.String,
                arrayOf = false
              ),
              JadeColumn(
                name = new JadeIdentifier("comments"),
                datatype = Datatype.String,
                arrayOf = true
              )
            ),
            primaryKey =
              Set(new JadeIdentifier("sample_id"), new JadeIdentifier("file_type"))
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
    )

    JadeDatasetGenerator
      .generateDataset(
        expected.name,
        expected.description,
        expected.defaultProfileId,
        sourceTables
      )
      .right
      .value shouldBe expected
  }

  it should "fail to build datasets with invalid relationships" in {
    // Missing the samples table referred to by the files table.
    val sourceTables = List(participants, files)

    val err = JadeDatasetGenerator
      .generateDataset(new JadeIdentifier("foo"), "bar", UUID.randomUUID(), sourceTables)
      .left
      .value

    err should include(samples.name.id)
    err should include("id")
  }
}
