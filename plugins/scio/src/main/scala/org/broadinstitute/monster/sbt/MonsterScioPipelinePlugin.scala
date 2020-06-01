package org.broadinstitute.monster.sbt

import com.typesafe.sbt.packager.universal.UniversalPlugin
import sbt._
import sbt.Keys._

/** Plugin for projects which build a Scio ETL pipeline. */
object MonsterScioPipelinePlugin extends AutoPlugin {
  override def requires: Plugins = MonsterDockerPlugin

  val ScioUtilsVersion = "1.4.0"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Set best-practice compiler flags for Scio.
    scalacOptions ++= Seq(
      "-Xmacro-settings:show-coder-fallback=true",
      "-language:higherKinds"
    ),
    // Add our common utils library.
    libraryDependencies ++= Seq(
      "org.broadinstitute.monster" %% "scio-utils" % ScioUtilsVersion,
      "org.broadinstitute.monster" %% "scio-test-utils" % ScioUtilsVersion % s"${Test.name},${IntegrationTest.name}"
    ),
    // Disable scio's annoying automatic version check.
    javaOptions += "-Dscio.ignoreVersionWarning=true",
    UniversalPlugin.autoImport.Universal / javaOptions += "-Dscio.ignoreVersionWarning=true",
    // FIXME: Kludge in a fix for a transitive dependency error in Scio 0.9.0.
    // See https://github.com/spotify/scio/issues/2945
    // This should go away when we pull in a scio-utils version w/ a fixed Scio dependency.
    dependencyOverrides += "com.google.guava" % "guava" % "27.0.1-jre"
  )
}
