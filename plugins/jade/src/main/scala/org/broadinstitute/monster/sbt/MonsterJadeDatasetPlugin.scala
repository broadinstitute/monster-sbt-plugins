package org.broadinstitute.monster.sbt

import java.nio.file.{Files, Path}
import java.util.UUID

import io.circe.syntax._
import io.circe.jawn.JawnParser
import org.broadinstitute.monster.sbt.model.MonsterTable
import sbt._
import sbt.Keys._
import sbt.complete.Parser
import sbt.complete.DefaultParsers._
import sbt.internal.util.ManagedLogger
import sbt.nio.Keys._
import sbt.nio.file.{FileAttributes, FileTreeView}

/** Plugin for projects which ETL data into a Jade dataset. */
object MonsterJadeDatasetPlugin extends AutoPlugin {
  override def requires: Plugins = MonsterBasePlugin

  object autoImport extends MonsterJadeDatasetKeys
  import autoImport._

  val uuidParser: Parser[UUID] = mapOrFail(NotSpace)(UUID.fromString)
  val jsonParser: JawnParser = new JawnParser()

  /**
    * Convert the contents of a local (non-Scala) file into Scala source code.
    *
    * The actual class-generation logic is passed into this method as an argument.
    * This method is useful because it provides common caching logic using sbt's
    * build-in file watching capabilities, re-generating classes only when their
    * source files change. Most of the logic here is copy-pasted from docs:
    * https://www.scala-sbt.org/1.x/docs/Howto-Track-File-Inputs-and-Outputs.html#File+inputs
    *
    * @param inputFiles list of files on disk which should be converted into Scala
    *                   source files
    * @param inputChanges description of filesystem changes since the last time
    *                     the task was triggered
    * @param inputExtension common file extension for each input file
    * @param outputDir directory where generated Scala classes should be written
    * @param fileView utility which can inspect the local filesystem
    * @param logger utility which can write logs to the sbt console
    * @param gen function which can convert input file contents to Scala source code
    */
  private def generateClasses(
    inputFiles: Seq[Path],
    inputChanges: FileChanges,
    inputExtension: String,
    outputDir: Path,
    fileView: FileTreeView[(Path, FileAttributes)],
    logger: ManagedLogger,
    gen: String => Either[Throwable, String]
  ): Seq[File] = {

    def outputPath(path: Path): Path =
      outputDir / path.getFileName.toString.replaceAll(s".$inputExtension$$", ".scala")

    def generate(path: Path): Path = {
      val input = new String(Files.readAllBytes(path))
      val output = outputPath(path)
      logger.info(s"Generating $output from $path")
      gen(input) match {
        case Left(err) =>
          logger.trace(err)
          sys.error(s"Failed to generate $output from $path")
        case Right(codeString) =>
          Files.write(output, codeString.getBytes())
      }
      output
    }

    // Build a mapping from expected-output-path -> source-input-path.
    val sourceMap = inputFiles.view.map(p => outputPath(p) -> p).toMap

    // Delete any files that exist in the output directory but don't
    // have a corresponding source file.
    val existingTargets = fileView
      .list(outputDir.toGlob / **)
      .flatMap {
        case (p, _) =>
          if (!sourceMap.contains(p)) {
            Files.deleteIfExists(p)
            None
          } else {
            Some(p)
          }
      }
      .toSet

    // Re-generate classes for new and updated source files.
    val toGenerate = (inputChanges.created ++ inputChanges.modified).toSet ++ sourceMap
      .filterKeys(!existingTargets.contains(_))
      .values
    toGenerate.foreach(generate)
    sourceMap.keys.toVector.map(_.toFile)
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    jadeTableSource := sourceDirectory.value / "jade-schema" / "tables",
    jadeTableExtension := "table.json",
    jadeTableTarget := (Compile / sourceManaged).value / "jade-schema" / "tables",
    jadeStructSource := sourceDirectory.value / "jade-schema" / "structs",
    jadeStructExtension := "struct.json",
    jadeStructTarget := (Compile / sourceManaged).value / "jade-schema" / "structs",
    Compile / managedSourceDirectories ++= Seq(
      jadeTableTarget.value,
      jadeStructTarget.value
    ),
    Compile / sourceGenerators ++= Seq(
      generateJadeTables.taskValue,
      generateJadeStructs.taskValue
    ),
    generateJadeTables / fileInputs += jadeTableSource.value.toGlob / s"*.${jadeTableExtension.value}",
    generateJadeStructs / fileInputs += jadeStructSource.value.toGlob / s"*.${jadeStructExtension.value}",
    generateJadeTables := generateClasses(
      inputFiles = generateJadeTables.inputFiles,
      inputChanges = generateJadeTables.inputFileChanges,
      inputExtension = jadeTableExtension.value,
      outputDir = jadeTableTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator
        .generateTableClass(jadeTablePackage.value, jadeStructPackage.value, _)
    ),
    generateJadeStructs := generateClasses(
      inputFiles = generateJadeStructs.inputFiles,
      inputChanges = generateJadeStructs.inputFileChanges,
      inputExtension = jadeStructExtension.value,
      outputDir = jadeStructTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator.generateStructClass(jadeStructPackage.value, _)
    ),
    generateJadeDataset := {
      val log = streams.value.log

      val profileId = (token(Space) ~> token(uuidParser, "<profile-id>")).parsed
      val name = jadeDatasetName.value
      val description = jadeDatasetDescription.value

      val sourcePattern = jadeTableSource.value.toGlob / s"*.${jadeTableExtension.value}"
      val sourceTables = FileTreeView.default.list(sourcePattern).map {
        case (path, _) =>
          jsonParser
            .decodeFile[MonsterTable](path.toFile)
            .fold(err => sys.error(err.getMessage), identity)
      }

      log.info(s"Generating Jade schema from ${sourceTables.length} input tables...")
      JadeDatasetGenerator
        .generateDataset(name, description, profileId, sourceTables)
        .fold(
          sys.error,
          datasetModel => {
            val out = target.value / s"$name.dataset.json"
            IO.write(out, datasetModel.asJson.noSpaces)
            log.info(s"Wrote Jade schema to ${out.getAbsolutePath}")
            out
          }
        )
    }
  )
}
