package org.broadinstitute.monster.sbt

import com.typesafe.sbt.packager.MappingsHelper
import com.typesafe.sbt.packager.linux.LinuxKeys
import org.broadinstitute.monster.sbt.model.{MonsterTable, MonsterTableFragment}
import sbt._
import sbt.Keys._
import sbt.nio.Keys._
import scoverage.ScoverageSbtPlugin

/** Plugin for projects which ETL data into a Jade dataset. */
object MonsterJadeDatasetPlugin extends AutoPlugin with LinuxKeys {
  import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
  import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
  import ScoverageSbtPlugin.autoImport._

  override def requires: Plugins = MonsterDockerPlugin

  object autoImport extends MonsterJadeDatasetKeys
  import autoImport._

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
    jadeTableFragmentSource := sourceDirectory.value / "main" / "jade-fragments",
    jadeTableFragmentExtension := "fragment.json",
    jadeTableFragmentTarget := (Compile / sourceManaged).value / "jade-schema" / "fragments",
    jadeStructSource := sourceDirectory.value / "main" / "jade-structs",
    jadeStructExtension := "struct.json",
    jadeStructTarget := (Compile / sourceManaged).value / "jade-schema" / "structs",
    Compile / managedSourceDirectories ++= Seq(
      jadeTableTarget.value,
      jadeTableFragmentTarget.value,
      jadeStructTarget.value
    ),
    Compile / sourceGenerators ++= Seq(
      generateJadeTables.taskValue,
      generateJadeTableFragments.taskValue,
      generateJadeStructs.taskValue
    ),
    generateJadeTables / fileInputs +=
      jadeTableSource.value.toGlob / s"*.${jadeTableExtension.value}",
    generateJadeTableFragments / fileInputs +=
      jadeTableFragmentSource.value.toGlob / s"*.${jadeTableFragmentExtension.value}",
    generateJadeStructs / fileInputs +=
      jadeStructSource.value.toGlob / s"*.${jadeStructExtension.value}",
    generateJadeTables := ClassGenerator.generateClasses(
      inputFiles = generateJadeTables.inputFiles,
      inputChanges = generateJadeTables.inputFileChanges,
      inputExtension = jadeTableExtension.value,
      outputDir = jadeTableTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator.generateTableClass[MonsterTable](
        jadeTablePackage.value,
        jadeTableFragmentPackage.value,
        jadeStructPackage.value,
        _
      )
    ),
    generateJadeTableFragments := ClassGenerator.generateClasses(
      inputFiles = generateJadeTableFragments.inputFiles,
      inputChanges = generateJadeTableFragments.inputFileChanges,
      inputExtension = jadeTableFragmentExtension.value,
      outputDir = jadeTableFragmentTarget.value.toPath,
      fileView = fileTreeView.value,
      logger = streams.value.log,
      gen = ClassGenerator.generateTableClass[MonsterTableFragment](
        // Not a typo, fragments get generated within the fragment package.
        jadeTableFragmentPackage.value,
        jadeTableFragmentPackage.value,
        jadeStructPackage.value,
        _
      )
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
    generateJadeSchema := JadeSchemaGenerator.generateSchema(
      inputDir = jadeTableSource.value,
      inputExtension = jadeTableExtension.value,
      fragmentDir = jadeTableFragmentSource.value,
      fragmentExtension = jadeTableFragmentExtension.value,
      outputDir = target.value,
      fileView = fileTreeView.value,
      logger = streams.value.log
    ),
    // Validate our JSON files on 'test'.
    Test / test := MonsterSchemaValidator.validateSchema(
      inputDir = jadeTableSource.value,
      inputExtension = jadeTableExtension.value,
      fragmentDir = jadeTableFragmentSource.value,
      fragmentExtension = jadeTableFragmentExtension.value,
      fileView = fileTreeView.value,
      logger = streams.value.log
    ),
    // Coverage of generated sources can break codedov.io, since it can't match the data to a source file.
    // NOTE: The types here leave a bit to be desired. This setting maps to this arg in the scoverage compiler
    // plugin, so we have to match its interface:
    // https://github.com/scoverage/scalac-scoverage-plugin#excluding-code-from-coverage-stats
    coverageExcludedPackages := List(
      coverageExcludedPackages.value,
      jadeTablePackage.value.replaceAllLiterally(".", "\\.") + ".*",
      jadeStructPackage.value.replaceAllLiterally(".", "\\.") + ".*"
    ).mkString(";"),
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
      fragmentDir = jadeTableFragmentSource.value,
      fragmentExtension = jadeTableFragmentExtension.value,
      outputDir = bigQueryMetadataTarget.value,
      fileView = fileTreeView.value,
      logger = streams.value.log
    )
  )
}
