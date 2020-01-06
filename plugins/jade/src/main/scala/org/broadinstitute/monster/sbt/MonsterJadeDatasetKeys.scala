package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.model.JadeIdentifier
import sbt._

/** sbt keys related to Jade dataset generation. */
trait MonsterJadeDatasetKeys {

  val jadeDatasetName: SettingKey[JadeIdentifier] =
    settingKey("Name of the Jade dataset modeled by this project")

  val jadeDatasetDescription: SettingKey[String] =
    settingKey("Description of the Jade dataset modeled by this project")

  val jadeTableSource: SettingKey[File] =
    settingKey("Directory containing table definitions for a Jade dataset")

  val jadeTableExtension: SettingKey[String] =
    settingKey("Extension for files containing Jade table definitions")

  val jadeTablePackage: SettingKey[String] =
    settingKey("Package which should be used for generated table classes")

  val jadeTableTarget: SettingKey[File] =
    settingKey("Directory where generated table classes should be written")

  val generateJadeTables: TaskKey[Seq[File]] =
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

  val generateJadeDataset: InputKey[File] = inputKey(
    "Generate JSON definition of a Jade dataset containing table definitions in this project"
  )
}
