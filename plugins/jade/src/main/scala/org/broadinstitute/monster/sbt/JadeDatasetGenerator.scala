package org.broadinstitute.monster.sbt

import java.nio.file.Path

import io.circe.jawn.JawnParser
import io.circe.syntax._
import sbt._
import java.util.UUID

import org.broadinstitute.monster.sbt.model._
import org.broadinstitute.monster.sbt.model.jadeapi._
import sbt.internal.util.ManagedLogger
import sbt.nio.file.{FileAttributes, FileTreeView}

/** Utilities for generating Jade dataset definitions from Monster table definitions. */
object JadeDatasetGenerator {
  private val jsonParser: JawnParser = new JawnParser()

  /**
   * Generate a JSON file containing a Jade dataset definition based on
   * the table definitions located in a local directory.
   *
   * @param name unique ID for the dataset
   * @param description human-friendly description for the dataset
   * @param profileId ID of the resource/billing profile the dataset should use
   * @param inputDir directory containing table definitions in our JSON format
   * @param inputExtension file extension used for our table definitions
   * @param outputDir directory where the Jade dataset request should be written
   * @param fileView utility which can inspect the local filesystem
   * @param logger utility which can write logs to the sbt console
   */
  def generateDataset(
    name: JadeIdentifier,
    description: String,
    profileId: UUID,
    inputDir: File,
    inputExtension: String,
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

    logger.info(s"Generating Jade schema from ${sourceTables.length} input tables")
    generateDataset(name, description, profileId, sourceTables) match {
      case Left(err) => sys.error(err)
      case Right(datasetModel) =>
        val out = outputDir / s"$name.dataset.json"
        IO.write(out, datasetModel.asJson.noSpaces)
        logger.info(s"Wrote Jade schema to ${out.getAbsolutePath}")
        out
    }
  }

  /**
    * Generate a Jade dataset from a collection of Monster tables.
    *
    * @param name unique ID for the dataset
    * @param description human-friendly description for the dataset
    * @param profileId ID of the resource/billing profile the dataset should use
    * @param tables collection of tables to include in the dataset
    */
  def generateDataset(
    name: JadeIdentifier,
    description: String,
    profileId: UUID,
    tables: Seq[MonsterTable]
  ): Either[String, JadeDataset] =
    extractRelationships(tables).map { rels =>
      JadeDataset(
        name = name,
        defaultProfileId = profileId,
        description = description,
        schema = JadeSchema(
          tables = tables.map(convertTable).toSet,
          relationships = rels.toSet
        )
      )
    }

  /**
    * Extract link fields from a collection of Monster tables, converting
    * them to Jade-compatible relationships.
    *
    * @param tables tables to extract links from
    */
  private def extractRelationships(
    tables: Seq[MonsterTable]
  ): Either[String, Seq[JadeRelationship]] = {
    val columnsPerTable = tables.map { baseTable =>
      baseTable.name -> baseTable.columns.map(_.name).toSet
    }.toMap

    tables.foldLeft[Either[String, List[JadeRelationship]]](Right(Nil)) { (acc, table) =>
      acc.flatMap { relationshipsSoFar =>
        val tableLinks = table.columns.flatMap(c => c.links.map(c.name -> _))
        val tableRelationships =
          tableLinks.foldLeft[Either[String, List[JadeRelationship]]](Right(Nil)) {
            case (acc, (sourceName, link)) =>
              acc.flatMap { relationshipsSoFar =>
                Either.cond(
                  columnsPerTable
                    .get(link.tableName)
                    .exists(_.contains(link.columnName)),
                  JadeRelationship(
                    from = JadeRelationshipRef(
                      table = table.name,
                      column = sourceName
                    ),
                    to = JadeRelationshipRef(
                      table = link.tableName,
                      column = link.columnName
                    )
                  ) :: relationshipsSoFar,
                  s"No such table/column pair: ${link.tableName}/${link.columnName}"
                )
              }
          }

        tableRelationships.map(_ ::: relationshipsSoFar)
      }
    }
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
    JadeTable(
      name = base.name,
      columns = (simpleColumns ++ structColumns).toSet,
      primaryKey = base.columns.collect {
        case col if col.`type` == ColumnType.PrimaryKey => col.name
      }.toSet
    )
  }
}
