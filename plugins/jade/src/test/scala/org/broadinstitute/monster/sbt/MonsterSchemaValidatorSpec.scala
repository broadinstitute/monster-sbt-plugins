package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.model._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MonsterSchemaValidatorSpec extends AnyFlatSpec with Matchers with EitherValues {

  private val common = MonsterTableFragment(
    name = new JadeIdentifier("common"),
    columns = Vector(
      SimpleColumn(
        name = new JadeIdentifier("id"),
        datatype = DataType.String,
        `type` = ColumnType.PrimaryKey
      )
    )
  )

  private val participants = MonsterTable(
    name = new JadeIdentifier("participant"),
    columns = Vector(
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
    tableFragments = Vector(common.name),
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
    ),
    tableFragments = Vector(common.name)
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
    tableFragments = Vector(common.name),
    partitioning = PartitionMode.DateFromColumn(new JadeIdentifier("creation_date"))
  )

  behavior of "MonsterSchemaValidator"

  it should "validate valid schemas" in {
    MonsterSchemaValidator
      .validateSchema(List(participants, samples, files), List(common)) shouldBe Right(())
  }

  it should "catch invalid fragment references" in {
    val errs = MonsterSchemaValidator.validateSchema(List(participants), Nil)
    errs.left.value should contain only "Table 'participant' references nonexistent fragment 'common'"
  }

  it should "catch collisions between table and fragment column names" in {
    val newCommon = common.copy(columns = common.columns :+ participants.columns.head)
    val errs = MonsterSchemaValidator.validateSchema(List(participants), List(newCommon))
    errs.left.value should contain only "Collision on column name 'age' in table 'participant'"
  }

  it should "catch invalid link tables" in {
    val errs = MonsterSchemaValidator.validateSchema(List(participants, files), List(common))
    errs.left.value should contain only "Link to invalid table 'sample' in table 'file'"
  }

  it should "catch invalid link columns" in {
    val newCommon = common.copy(
      columns = common.columns
        .map(_.copy(links = Vector(Link(participants.name, new JadeIdentifier("foo")))))
    )
    val errs = MonsterSchemaValidator.validateSchema(List(participants), List(newCommon))
    errs.left.value should contain only "Link to invalid table/column 'participant/foo' in table 'participant'"
  }
}
