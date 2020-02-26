package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._

/** Plugin for projects which build a Scio ETL pipeline. */
object MonsterScioPipelinePlugin extends AutoPlugin {
  override def requires: Plugins = MonsterDockerPlugin

  val ScioUtilsVersion = "1.1.1"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Set best-practice compiler flags for Scio.
    scalacOptions ++= Seq(
      "-Xmacro-settings:show-coder-fallback=true",
      "-language:higherKinds"
    ),
    // Add our common utils library.
    libraryDependencies ++= Seq(
      "org.broadinstitute.monster" %% "scio-utils" % ScioUtilsVersion,
      "org.broadinstitute.monster" %% "scio-test-utils" % ScioUtilsVersion % Test
    )
  )
}
