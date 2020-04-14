package org.broadinstitute.monster.sbt

import sbt._

trait MonsterHelmPluginKeys {

  val helmStagingDirectory: SettingKey[File] = settingKey(
    "Local directory where packaged Helm charts should be staged"
  )

  val helmChartLocalIndex: SettingKey[File] = settingKey(
    "Local path where index.yaml should be generated on Helm releases"
  )

  val helmChartOrganization: SettingKey[String] = settingKey(
    "GitHub organization of the chart repository where Helm charts should be published"
  )

  val helmChartRepository: SettingKey[String] = settingKey(
    "GitHub repository where Helm charts should be published"
  )

  val packageHelmChart: TaskKey[File] = taskKey("Package the project's Helm chart")

  val releaseHelmChart: TaskKey[Unit] = taskKey("Upload the project's Helm chart to GitHub")
}
