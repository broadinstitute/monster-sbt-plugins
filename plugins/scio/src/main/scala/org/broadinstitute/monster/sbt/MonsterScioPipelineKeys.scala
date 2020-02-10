package org.broadinstitute.monster.sbt

import sbt._

/** sbt keys related to Scio pipeline projects. */
trait MonsterScioPipelineKeys {

  val scioSnapshotBucketName: SettingKey[String] =
    settingKey("Name of the GCS bucket where pipeline snapshots should be uploaded")

  val scioReleaseBucketName: SettingKey[String] =
    settingKey("Name of the GCS bucket where pipeline releases should be uploaded")
}
