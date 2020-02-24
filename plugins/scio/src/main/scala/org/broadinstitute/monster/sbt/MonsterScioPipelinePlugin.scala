package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._

/** Plugin for projects which build a Scio ETL pipeline. */
object MonsterScioPipelinePlugin extends AutoPlugin {
  override def requires: Plugins = MonsterDockerPlugin

  val ScioUtilsVersion = "1.0.0"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Set best-practice compiler flags for Scio.
    scalacOptions ++= Seq(
      "-Xmacro-settings:show-coder-fallback=true",
      "-language:higherKinds"
    ),
    // Add our common utils library.
    // TODO: Are there any common test libraries that we could/should
    // also add here?
    libraryDependencies += "org.broadinstitute.monster" %% "scio-utils" % ScioUtilsVersion
  )
}