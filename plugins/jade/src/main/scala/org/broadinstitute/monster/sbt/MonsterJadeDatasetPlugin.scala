package org.broadinstitute.monster.sbt

import java.nio.file.{Files, Path}

import sbt._
import sbt.Keys._
import sbt.nio.Keys._

/** Plugin for projects which ETL data into a Jade dataset. */
object MonsterJadeDatasetPlugin extends AutoPlugin {
  override def requires: Plugins = MonsterBasePlugin

  object autoImport {

    val jadeSchemaSource: SettingKey[File] =
      settingKey("Directory containing table definitions for a Jade dataset")

    val jadeSchemaExtension: SettingKey[String] =
      settingKey("Extension for files containing Jade table definitions")

    val jadeTablePackage: SettingKey[String] =
      settingKey("Package which should be used for generated table classes")

    val jadeSchemaTarget: SettingKey[File] =
      settingKey("Directory where generated table classes should be written")

    val generateJadeTables: TaskKey[Seq[File]] =
      taskKey("Generate case classes for table definitions in the project")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    jadeSchemaSource := sourceDirectory.value / "jade-schema",
    jadeSchemaExtension := "schema.json",
    jadeSchemaTarget := (Compile / sourceManaged).value / "jade-schema",
    Compile / managedSourceDirectories += jadeSchemaTarget.value,
    generateJadeTables / fileInputs += jadeSchemaSource.value.toGlob / s"*.${jadeSchemaExtension.value}",
    generateJadeTables := {
      /*
       * To avoid pointless churn, we use sbt's built-in file watching capabilities
       * to only re-generate classes when their source schemas change.
       *
       * Most of the code here is copy-pasted from this documentation:
       * https://www.scala-sbt.org/1.x/docs/Howto-Track-File-Inputs-and-Outputs.html#File+inputs
       */
      val outputDir = Files.createDirectories(jadeSchemaTarget.value.toPath)
      val logger = streams.value.log

      def outputPath(path: Path): Path =
        outputDir / path.getFileName.toString
          .replaceAll(s".${jadeSchemaExtension.value}$$", ".scala")

      def generate(path: Path): Path = {
        val input = new String(Files.readAllBytes(path))
        val output = outputPath(path)
        logger.info(s"Generating $output from $path")
        JadeTableGenerator.generateTableClass(jadeTablePackage.value, input) match {
          case Left(err) =>
            logger.trace(err)
            sys.error(s"Failed to generate $output from $path")
          case Right(codeString) =>
            Files.write(output, codeString.getBytes())
        }
        output
      }

      // Build a mapping from expected-output-path -> source-input-path.
      val sourceMap =
        generateJadeTables.inputFiles.view.map(p => outputPath(p) -> p).toMap

      // Delete any files that exist in the output directory but don't
      // have a corresponding source file.
      val existingTargets = fileTreeView.value
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
      val changes = generateJadeTables.inputFileChanges
      val updatedPaths = (changes.created ++ changes.modified).toSet
      val toGenerate = updatedPaths ++ sourceMap
        .filterKeys(!existingTargets.contains(_))
        .values
      toGenerate.foreach(generate)
      sourceMap.keys.toVector.map(_.toFile)
    },
    Compile / sourceGenerators += generateJadeTables.taskValue
  )
}
