package org.broadinstitute.monster.sbt

import org.broadinstitute.monster.sbt.model.JadeIdentifier
import sbt._

/** sbt keys related to Jade dataset generation. */
trait MonsterJadeDatasetKeys {

  val jadeDatasetName: SettingKey[JadeIdentifier] =
    settingKey("Name of the Jade dataset modeled by this project")

  val jadeDatasetDescription: SettingKey[String] =
    settingKey("Description of the Jade dataset modeled by this project")

  val jadeSchemaSource: SettingKey[File] =
    settingKey("Directory containing table definitions for a Jade dataset")

  val jadeSchemaExtension: SettingKey[String] =
    settingKey("Extension for files containing Jade table definitions")

  val jadeTablePackage: SettingKey[String] =
    settingKey("Package which should be used for generated table classes")

  val jadeSchemaTarget: SettingKey[File] =
    settingKey("Directory where generated table classes should be written")

  val generateJadeTables: TaskKey[Seq[File]] =
    taskKey("Generate case classes for table definitions in this project")

  val generateJadeDataset: InputKey[File] = inputKey(
    "Generate JSON definition of a Jade dataset containing table definitions in this project"
  )
}
