package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._
import sbtassembly.{AssemblyKeys, AssemblyPlugin}

import scala.sys.process.Process

/** Plugin for projects which build a Scio ETL pipeline. */
object MonsterScioPipelinePlugin extends AutoPlugin with AssemblyKeys {
  override def requires: Plugins = MonsterBasePlugin && AssemblyPlugin

  object autoImport extends MonsterScioPipelineKeys
  import autoImport._

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
    libraryDependencies += "org.broadinstitute.monster" %% "scio-utils" % ScioUtilsVersion,
    // Decouple testing from JAR-building.
    assembly / test := {},
    // Rewire publish to upload self-contained pipeline JARs to GCS.
    publish := {
      val artifact = assembly.value

      val targetBucket = if (isSnapshot.value) {
        scioSnapshotBucketName.value
      } else {
        scioReleaseBucketName.value
      }
      val projectName = name.value
      val releaseVersion = version.value
      val targetPath = s"gs://$targetBucket/$projectName/$projectName-$releaseVersion.jar"

      val copyCode =
        Process(s"gsutil cp ${artifact.absolutePath} $targetPath").run().exitValue()

      if (copyCode != 0) {
        sys.error(
          s"Failed to upload $projectName release to GCS, got exit code: $copyCode"
        )
      }
    }
  )
}
