package org.broadinstitute.monster.sbt

import java.nio.file.Path

import io.circe.jawn.JawnParser
import io.circe.syntax._
import org.broadinstitute.monster.sbt.model.ColumnType.{Optional, _}
import org.broadinstitute.monster.sbt.model.bigquery.BigQueryColumn
import org.broadinstitute.monster.sbt.model.{MonsterTable, MonsterTableFragment, SimpleColumn}
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
    * @param fragmentDir directory containing partial table definitions in our JSON format
    * @param fragmentExtension file extension used for partial table definitions
    * @param outputDir directory where BQ metadata files should be written
    * @param fileView utility which can inspect the local filesystem
    * @param logger utility which can write logs to the sbt console
    */
  def generateMetadata(
    inputDir: File,
    inputExtension: String,
    fragmentDir: File,
    fragmentExtension: String,
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

    val fragmentPattern = fragmentDir.toGlob / s"*.$fragmentExtension"
    val fragments = fileView.list(fragmentPattern).map {
      case (path, _) =>
        jsonParser
          .decodeFile[MonsterTableFragment](path.toFile)
          .fold(err => sys.error(err.getLocalizedMessage), identity)
    }

    // Clean the directory before generating anything.
    IO.delete(outputDir)

    // Make sure the schema is valid.
    MonsterSchemaValidator
      .validateSchema(sourceTables, fragments)
      .fold(
        errs =>
          sys.error(
            s"Cannot generate BigQuery metadata because of ${errs.length} validation errors"
          ),
        identity
      )

    // Generate new files.
    val fragmentMap = fragments.map(f => f.name -> f).toMap
    sourceTables.flatMap { table =>
      logger.info(s"Generating BigQuery metadata for table ${table.name}")
      val fragments = table.tableFragments.foldLeft(List.empty[MonsterTableFragment]) { (acc, id) =>
        fragmentMap(id) :: acc
      }
      val schema = tableSchema(table, fragments)
      val pkCols = primaryKeyColumns(table, fragments)
      val compareCols = nonPrimaryKeyColumns(table, fragments)

      val out = outputDir / table.name.id
      val schemaOut = out / "schema.json"
      val pkOut = out / "primary-keys"
      val compareOut = out / "compare-cols"

      IO.write(schemaOut, schema.asJson.noSpaces)
      IO.write(pkOut, pkCols.mkString(","))
      IO.write(compareOut, compareCols.mkString(","))
      logger.info(s"Wrote BigQuery metadata to ${out.getAbsolutePath}/")

      Seq(schemaOut, pkOut, compareOut)
    }
  }

  /** Get a BigQuery schema describing the columns of one of our tables. */
  def tableSchema(
    table: MonsterTable,
    includedFragments: Seq[MonsterTableFragment]
  ): BigQueryTable = {
    val fragmentColumns = includedFragments.flatMap(_.columns)
    val simpleColumns = (table.columns ++ fragmentColumns).map { column =>
      BigQueryColumn(
        name = column.name.id,
        `type` = column.datatype.asBigQuery,
        mode = column.`type`.asBigQuery
      )
    }
    val fragmentStructs = includedFragments.flatMap(_.structColumns)
    val structColumns = (table.structColumns ++ fragmentStructs).map { column =>
      BigQueryColumn(
        name = column.name.id,
        `type` = "STRING",
        mode = column.`type`.asBigQuery
      )
    }
    simpleColumns ++ structColumns
  }

  /** Get the names of all primary-key columns in one of our tables. */
  def primaryKeyColumns(
    table: MonsterTable,
    includedFragments: Seq[MonsterTableFragment]
  ): Seq[String] = {
    val fragmentColumns = includedFragments.flatMap(_.columns)
    (table.columns ++ fragmentColumns).collect {
      case SimpleColumn(name, _, PrimaryKey, _) => name.id
    }
  }

  /** Get the names of all non-primary-key columns in one of our tables. */
  def nonPrimaryKeyColumns(
    table: MonsterTable,
    includedFragments: Seq[MonsterTableFragment]
  ): Seq[String] = {
    val fragmentColumns = includedFragments.flatMap(_.columns)
    val simpleCols = (table.columns ++ fragmentColumns).collect {
      case SimpleColumn(name, _, Required | Repeated | Optional, _) => name.id
    }
    val fragmentStructs = includedFragments.flatMap(_.structColumns)
    val structCols = (table.structColumns ++ fragmentStructs).map(_.name.id)

    simpleCols ++ structCols
  }
}
