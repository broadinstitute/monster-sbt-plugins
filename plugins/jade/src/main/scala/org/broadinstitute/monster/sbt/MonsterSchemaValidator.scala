package org.broadinstitute.monster.sbt

import java.nio.file.Path

import io.circe.jawn.JawnParser
import org.broadinstitute.monster.sbt.model.{MonsterTable, MonsterTableFragment}
import sbt._
import sbt.internal.util.ManagedLogger
import sbt.nio.file.{FileAttributes, FileTreeView}

object MonsterSchemaValidator {
  private val jsonParser: JawnParser = new JawnParser()

  /**
    * Validate a Jade schema definition as specified by JSON on local disk.
    *
    * @param inputDir directory containing table definitions in our JSON format
    * @param inputExtension file extension used for our table definitions
    * @param fragmentDir directory containing partial table definitions in our JSON format
    * @param fragmentExtension file extension used for partial table definitions
    * @param fileView utility which can inspect the local filesystem
    * @param logger utility which can write logs to the sbt console
    */
  def validateSchema(
    inputDir: File,
    inputExtension: String,
    fragmentDir: File,
    fragmentExtension: String,
    fileView: FileTreeView[(Path, FileAttributes)],
    logger: ManagedLogger
  ): Unit = {
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

    logger.info(s"Validating ${sourceTables.length} tables and ${fragments.length} fragments...")
    validateSchema(sourceTables, fragments) match {
      case Right(()) => logger.info("Validation passed")
      case Left(errs) =>
        errs.foreach(e => logger.err(s"Validation error: $e"))
        sys.error(s"Validation failed with ${errs.length} errors")
    }
  }

  /**
    * Validate a Jade schema definition as specified by a set of table models, potentially
    * referencing elements from a set of table fragments.
    */
  def validateSchema(
    tables: Seq[MonsterTable],
    fragments: Seq[MonsterTableFragment]
  ): Either[Seq[String], Unit] = {
    val fragmentMap = fragments.map(f => f.name -> f).toMap
    val columnsByTable = tables.map(t => t.name -> t.columns.map(_.name).toSet).toMap

    val errs = tables.foldLeft(List.empty[String]) { (errAcc, table) =>
      val tableId = table.name

      val (idErrs, linkedFragments) = table.tableFragments.map { fragmentId =>
        fragmentMap.get(fragmentId) match {
          case Some(fragment) => (None, Some(fragment))
          case None           => (Some(s"Table $tableId references nonexistent fragment $fragmentId"), None)
        }
      }.unzip

      val tableCols = table.columns.map(_.name) ++ table.structColumns.map(_.name)
      val fragmentCols =
        linkedFragments.flatten.flatMap(f => f.columns.map(_.name) ++ f.structColumns.map(_.name))

      val collisionErrs = tableCols.intersect(fragmentCols).map { collidingName =>
        s"Collision on column name $collidingName in table $tableId"
      }

      val allLinks = table.columns.flatMap(_.links) ++
        linkedFragments.flatten.flatMap(_.columns.flatMap(_.links))

      val linkErrs = allLinks.flatMap { link =>
        columnsByTable.get(link.tableName) match {
          case None => Some(s"Link to invalid table ${link.tableName} in table $tableId")
          case Some(cols) =>
            if (cols.contains(link.columnName)) {
              None
            } else {
              Some(
                s"Link to invalid table/column ${link.tableName}/${link.columnName} in table $tableId"
              )
            }
        }
      }

      linkErrs.toList ::: collisionErrs.toList ::: idErrs.flatten.toList ::: errAcc
    }

    if (errs.isEmpty) Right(()) else Left(errs)
  }
}
