package org.broadinstitute.monster.sbt

import sbt._

/** TODO */
object MonsterJadeDatasetPlugin extends AutoPlugin {
  override def requires: Plugins = MonsterBasePlugin

  object autoImport {

    val schemaSource: SettingKey[File] =
      settingKey("Directory containing table definitions for a Jade dataset")

    val tablePackage: SettingKey[String] =
      settingKey("Package which should be used for generated table classes")

    val generateTables: TaskKey[Seq[File]] =
      taskKey("Generate case classes for table definitions in the project")
  }

  override def projectSettings: Seq[Def.Setting[_]] = super.projectSettings
}
