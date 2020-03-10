package org.broadinstitute.monster.sbt

import java.nio.file.Path

import io.circe.jawn.JawnParser
import io.circe.syntax._
import org.broadinstitute.monster.sbt.model.ColumnType.{Optional, _}
import org.broadinstitute.monster.sbt.model.bigquery.BigQueryColumn
import org.broadinstitute.monster.sbt.model.{MonsterTable, SimpleColumn}
import sbt._
import sbt.internal.util.ManagedLogger
import sbt.nio.file.{FileAttributes, FileTreeView}

/** Utilities for generating BigQuery metadata from Monster table definitions. */
object BigQueryMetadataGenerator {
  private val jsonParser: JawnParser = new JawnParser()

  type BigQueryTable = Seq[BigQueryColumn]

  /**
    * Generate BigQuery metadata files based on the table definitions
    * located in a local directory.
    *
    * @param inputDir directory containing table definitions in our JSON format
    * @param inputExtension file extension used for our table definitions
    * @param outputDir directory where BQ metadata files should be written
    * @param fileView utility which can inspect the local filesystem
    * @param logger utility which can write logs to the sbt console
    */
  def generateMetadata(
    inputDir: File,
    inputExtension: String,
    outputDir: File,
    fileView: FileTreeView[(Path, FileAttributes)],
    logger: ManagedLogger
  ): Seq[File] = {
    val sourcePattern = inputDir.toGlob / s"*.$inputExtension"
    val sourceTables = fileView.list(sourcePattern).map {
      case (path, _) =>
        jsonParser
          .decodeFile[MonsterTable](path.toFile)
          .fold(err => sys.error(err.getMessage), identity)
    }

    sourceTables.flatMap { table =>
      logger.info(s"Generating BigQuery metadata for table ${table.name}")
      val schema = tableSchema(table)
      val pkCols = primaryKeyColumns(table)
      val compareCols = nonPrimaryKeyColumns(table)

      val out = outputDir / table.name.id
      val schemaOut = out / "schema.json"
      val pkOut = out / "primary-keys"
      val compareOut = out / "compare-cols"

      out.mkdirs()
      IO.write(schemaOut, schema.asJson.noSpaces)
      IO.write(pkOut, pkCols.mkString(","))
      IO.write(compareOut, compareCols.mkString(","))
      logger.info(s"Wrote BigQuery metadata to ${out.getAbsolutePath}/")

      Seq(schemaOut, pkOut, compareOut)
    }
  }

  /** Get a BigQuery schema describing the columns of one of our tables. */
  def tableSchema(table: MonsterTable): BigQueryTable =
    table.columns.map { column =>
      BigQueryColumn(
        name = column.name.id,
        `type` = column.datatype.asBigQuery,
        mode = column.`type`.asBigQuery
      )
    }

  /** Get the names of all primary-key columns in one of our tables. */
  def primaryKeyColumns(table: MonsterTable): Seq[String] =
    table.columns.collect {
      case SimpleColumn(name, _, PrimaryKey, _) => name.id
    }

  /** Get the names of all non-primary-key columns in one of our tables. */
  def nonPrimaryKeyColumns(table: MonsterTable): Seq[String] = {
    val simpleCols = table.columns.collect {
      case SimpleColumn(name, _, Required | Repeated | Optional, _) => name.id
    }
    val structCols = table.structColumns.map(_.name.id)

    simpleCols ++ structCols
  }
}
