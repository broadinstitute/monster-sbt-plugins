package org.broadinstitute.monster.sbt

import java.util.UUID

import com.typesafe.sbt.packager.MappingsHelper
import com.typesafe.sbt.packager.linux.LinuxKeys
import sbt._
import sbt.Keys._
import sbt.complete.Parser
import sbt.complete.DefaultParsers._
import sbt.nio.Keys._

/** Plugin for projects which ETL data into a Jade dataset. */
object MonsterJadeDatasetPlugin extends AutoPlugin with LinuxKeys {
  import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
  import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

  override def requires: Plugins = MonsterDockerPlugin

  object autoImport extends MonsterJadeDatasetKeys
  import autoImport._

  private val uuidParser: Parser[UUID] = mapOrFail(NotSpace)(UUID.fromString)

  // We inject circe as a dependency so we can include serialization
  // logic within generated source code.
  val CirceVersion = "0.12.3"
  val CirceDerivationVersion = "0.12.0-M7"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-derivation" % CirceDerivationVersion
    ),
    jadeTableSource := sourceDirectory.value / "main" / "jade-tables",
    jadeTableExtension := "table.json",
    jadeTableTarget := (Compile / sourceManaged).value / "jade-schema" / "tables",
    jadeStructSource := sourceDirectory.value / "main" / "jade-structs",
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
    generateJadeTables := ClassGenerator.generateClasses(
      inputFiles = generateJadeTables.inputFiles,
      inputChanges = generateJadeTables.inputFileChanges,
      inputExtension = jadeTableExtension.value,
      outputDir = jadeTableTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator
        .generateTableClass(jadeTablePackage.value, jadeStructPackage.value, _)
    ),
    generateJadeStructs := ClassGenerator.generateClasses(
      inputFiles = generateJadeStructs.inputFiles,
      inputChanges = generateJadeStructs.inputFileChanges,
      inputExtension = jadeStructExtension.value,
      outputDir = jadeStructTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator.generateStructClass(jadeStructPackage.value, _)
    ),
    generateJadeDataset := JadeDatasetGenerator.generateDataset(
      name = jadeDatasetName.value,
      description = jadeDatasetDescription.value,
      profileId = (token(Space) ~> token(uuidParser, "<profile-id>")).parsed,
      inputDir = jadeTableSource.value,
      inputExtension = jadeTableExtension.value,
      outputDir = target.value,
      fileView = fileTreeView.value,
      logger = streams.value.log
    ),
    // Override the Docker image to use gcloud, so we get access to 'bq'.
    dockerBaseImage := "google/cloud-sdk:283.0.0-slim",
    dockerEntrypoint := Seq("bq"),
    // Write local files into the /schemas directory.
    Docker / defaultLinuxInstallLocation := "/bq-metadata",
    // Rewire the Docker mappings to only add generated metadata files.
    bigQueryMetadataTarget := target.value / "bq",
    Universal / mappings := {
      val dir = bigQueryMetadataTarget.value
      val metadataFiles = generateBigQueryMetadata.value
      metadataFiles.pair(MappingsHelper.relativeTo(dir))
    },
    generateBigQueryMetadata := BigQueryMetadataGenerator.generateMetadata(
      inputDir = jadeTableSource.value,
      inputExtension = jadeTableExtension.value,
      outputDir = bigQueryMetadataTarget.value,
      fileView = fileTreeView.value,
      logger = streams.value.log
    )
  )
}
