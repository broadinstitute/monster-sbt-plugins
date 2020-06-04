package org.broadinstitute.monster.sbt

import sbt._

/** sbt keys related to Jade dataset generation. */
trait MonsterJadeDatasetKeys {

  val jadeTableSource: SettingKey[File] =
    settingKey("Directory containing table-fragment definitions for a Jade dataset")

  val jadeTableExtension: SettingKey[String] =
    settingKey("Extension for files containing Jade table-fragment definitions")

  val jadeTablePackage: SettingKey[String] =
    settingKey("Package which should be used for generated table-fragment classes")

  val jadeTableTarget: SettingKey[File] =
    settingKey("Directory where generated table-fragment classes should be written")

  val generateJadeTables: TaskKey[Seq[File]] =
    taskKey("Generate case classes for table-fragment definitions in this project")

  val jadeTableFragmentSource: SettingKey[File] =
    settingKey("Directory containing table definitions for a Jade dataset")

  val jadeTableFragmentExtension: SettingKey[String] =
    settingKey("Extension for files containing Jade table definitions")

  val jadeTableFragmentPackage: SettingKey[String] =
    settingKey("Package which should be used for generated table classes")

  val jadeTableFragmentTarget: SettingKey[File] =
    settingKey("Directory where generated table classes should be written")

  val generateJadeTableFragments: TaskKey[Seq[File]] =
    taskKey("Generate case classes for table definitions in this project")

  val jadeStructSource: SettingKey[File] =
    settingKey("Directory containing struct definitions for a Jade dataset")

  val jadeStructExtension: SettingKey[String] =
    settingKey("Extension for files containing Jade struct definitions")

  val jadeStructPackage: SettingKey[String] =
    settingKey("Package which should be used for generated struct classes")

  val jadeStructTarget: SettingKey[File] =
    settingKey("Directory where generated struct classes should be written")

  val generateJadeStructs: TaskKey[Seq[File]] =
    taskKey("Generate case classes for struct definitions in this project")

  val generateJadeSchema: TaskKey[File] = taskKey(
    "Generate JSON definition of a Jade dataset containing table definitions in this project"
  )

  val bigQueryMetadataTarget: SettingKey[File] =
    settingKey("Directory where generated BigQuery metadata should be written")

  val generateBigQueryMetadata: TaskKey[Seq[File]] =
    taskKey("Generate BigQuery metadata files for table definitions in this project")
}
