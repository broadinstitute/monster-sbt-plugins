package org.broadinstitute.monster.sbt

import com.typesafe.sbt.packager.NativePackagerKeys
import com.typesafe.sbt.packager.archetypes.scripts.AshScriptPlugin
import com.typesafe.sbt.packager.docker.{DockerPlugin => NativeDockerPlugin}
import com.typesafe.sbt.packager.linux.LinuxKeys
import sbt._
import sbt.Keys._

/**
  * Plugin which should be applied to Monster sub-projects that need to be
  * published as Docker images for use in pipelines.
  */
object DockerPlugin extends AutoPlugin with LinuxKeys with NativePackagerKeys {
  import NativeDockerPlugin.autoImport._

  override def requires = NativeDockerPlugin && AshScriptPlugin && BasePlugin

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := "openjdk:8-alpine",
    dockerRepository := Some("us.gcr.io/broad-dsp-gcr-public"),
    dockerLabels := Map("VERSION" -> version.value),
    Docker / defaultLinuxInstallLocation := "/app",
    Docker / maintainer := "monster@broadinstitute.org",
    // Make our CI life easier and set up publish delegation here.
    publish := (Docker / publish).value
  )
}
