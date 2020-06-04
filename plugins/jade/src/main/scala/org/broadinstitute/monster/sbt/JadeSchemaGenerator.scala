package org.broadinstitute.monster.sbt

import java.nio.file.Path

import io.circe.jawn.JawnParser
import io.circe.syntax._
import sbt._

import org.broadinstitute.monster.sbt.model._
import org.broadinstitute.monster.sbt.model.jadeapi._
import sbt.internal.util.ManagedLogger
import sbt.nio.file.{FileAttributes, FileTreeView}

/** Utilities for generating Jade schema definitions from Monster table definitions. */
object JadeSchemaGenerator {
  private val jsonParser: JawnParser = new JawnParser()

  /**
    * Generate a JSON file containing a Jade schema definition based on
    * the table definitions located in a local directory.
    *
    * @param inputDir directory containing table definitions in our JSON format
    * @param inputExtension file extension used for our table definitions
    * @param fragmentDir directory containing partial table definitions in our JSON format
    * @param fragmentExtension file extension used for partial table definitions
    * @param outputDir directory where the Jade schema should be written
    * @param fileView utility which can inspect the local filesystem
    * @param logger utility which can write logs to the sbt console
    */
  def generateSchema(
    inputDir: File,
    inputExtension: String,
    fragmentDir: File,
    fragmentExtension: String,
    outputDir: File,
    fileView: FileTreeView[(Path, FileAttributes)],
    logger: ManagedLogger
  ): File = {
    val sourcePattern = inputDir.toGlob / s"*.$inputExtension"
    val sourceTables = fileView.list(sourcePattern).map {
      case (path, _) =>
        jsonParser
          .decodeFile[MonsterTable](path.toFile)
          .fold(err => sys.error(err.getMessage), identity)
    }

    val fragmentPattern = fragmentDir.toGlob / s"*.$fragmentExtension"
    val fragments = fileView.list(fragmentPattern).map {
      case (path, _) =>
        jsonParser
          .decodeFile[MonsterTableFragment](path.toFile)
          .fold(err => sys.error(err.getLocalizedMessage), identity)
    }

    MonsterSchemaValidator
      .validateSchema(sourceTables, fragments)
      .fold(
        errs =>
          sys.error(s"Cannot generate Jade schema because of ${errs.length} validation errors"),
        identity
      )

    logger.info(s"Generating Jade schema from ${sourceTables.length} input tables")
    val schemaModel = generateSchema(sourceTables, fragments)
    val out = outputDir / "schema.json"
    IO.write(out, schemaModel.asJson.noSpaces)
    logger.info(s"Wrote Jade schema to ${out.getAbsolutePath}")
    out
  }

  /**
    * Generate a Jade schema from a collection of Monster tables and
    * potentially-referenced fragments.
    *
    * NOTE: Assumes there are no dangling references in the tables or fragments.
    * Validate the schema before calling this method if that assumption doesn't hold.
    */
  def generateSchema(
    tables: Seq[MonsterTable],
    fragments: Seq[MonsterTableFragment]
  ): JadeSchema = {
    val fragmentMap = fragments.map(t => t.name -> t).toMap

    val flattenedTables = tables.foldLeft(List.empty[MonsterTable]) { (acc, table) =>
      flattenFragments(table, fragmentMap) :: acc
    }

    JadeSchema(
      tables = flattenedTables.map(convertTable).toSet,
      relationships = extractRelationships(flattenedTables).toSet
    )
  }

  /** "Flatten" any fragments referenced by a table by pulling their columns to the top level. */
  private def flattenFragments(
    table: MonsterTable,
    fragments: Map[JadeIdentifier, MonsterTableFragment]
  ): MonsterTable =
    table.tableFragments.foldLeft(table.copy(tableFragments = Vector.empty)) { (acc, fragmentId) =>
      val subTable = fragments(fragmentId)
      acc.copy(
        columns = acc.columns ++ subTable.columns,
        structColumns = acc.structColumns ++ subTable.structColumns
      )
    }

  /**
    * Extract link fields from a collection of Monster tables, converting
    * them to Jade-compatible relationships.
    *
    * Assumes there are no dangling links.
    */
  private def extractRelationships(tables: Seq[MonsterTable]): Seq[JadeRelationship] =
    tables.foldLeft(List.empty[JadeRelationship]) { (acc, table) =>
      val tableLinks = table.columns.flatMap(c => c.links.map(c.name -> _))
      val tableRelationships = tableLinks.foldLeft(List.empty[JadeRelationship]) {
        case (relationshipsSoFar, (sourceName, link)) =>
          val relationship = JadeRelationship(
            from = JadeRelationshipRef(
              table = table.name,
              column = sourceName
            ),
            to = JadeRelationshipRef(
              table = link.tableName,
              column = link.columnName
            )
          )
          relationship :: relationshipsSoFar
      }

      tableRelationships ::: acc
    }

  /** Convert a Monster table to a Jade-compatible table. */
  private def convertTable(base: MonsterTable): JadeTable = {
    val simpleColumns = base.columns.map { baseCol =>
      JadeColumn(
        name = baseCol.name,
        datatype = baseCol.datatype,
        arrayOf = baseCol.`type` == ColumnType.Repeated
      )
    }
    val structColumns = base.structColumns.map { structCol =>
      JadeColumn(
        name = structCol.name,
        datatype = DataType.String,
        arrayOf = structCol.`type` == ColumnType.Repeated
      )
    }
    val (mode, dateOpts, intOpts) = base.partitioning match {
      case PartitionMode.IngestDate =>
        (JadePartitionMode.Date, Some(JadeDatePartitionOptions.IngestDate), None)
      case PartitionMode.DateFromColumn(col) =>
        (JadePartitionMode.Date, Some(JadeDatePartitionOptions(col)), None)
      case PartitionMode.IntRangeFromColumn(col, min, max, interval) =>
        (
          JadePartitionMode.Int,
          None,
          Some(JadeIntPartitionOptions(col, min, max, interval))
        )
    }

    JadeTable(
      name = base.name,
      columns = (simpleColumns ++ structColumns).toSet,
      primaryKey = base.columns.collect {
        case col if col.`type` == ColumnType.PrimaryKey => col.name
      }.toSet,
      partitionMode = mode,
      datePartitionOptions = dateOpts,
      intPartitionOptions = intOpts
    )
  }
}
