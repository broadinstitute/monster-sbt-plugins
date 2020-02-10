package org.broadinstitute.monster.sbt

import com.google.cloud.storage.{BlobInfo, StorageOptions}
import sbt._
import sbt.Keys._
import sbtassembly.{AssemblyKeys, AssemblyPlugin}

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
      val log = streams.value.log

      // Package the stand-alone JAR to upload.
      val artifact = assembly.value

      // Determine the upload path for the JAR.
      val targetBucket = if (isSnapshot.value) {
        scioSnapshotBucketName.value
      } else {
        scioReleaseBucketName.value
      }
      val projectName = name.value
      val releaseVersion = version.value
      val targetPath = s"$projectName/$projectName-$releaseVersion.jar"

      // Run the upload.
      // NOTE: This will use the GCP credentials in the environment.
      // CI will need to pull service account info from Vault to use this.
      log.info(s"Uploading ${artifact.absolutePath} to gs://$targetBucket/$targetPath...")
      val storage = StorageOptions.getDefaultInstance.getService
      val targetBlob = BlobInfo
        .newBuilder(targetBucket, targetPath)
        .setContentType("application/java-archive")
        .build()
      storage.create(targetBlob, IO.readBytes(artifact))
      log.info(s"Upload to gs://$targetBucket/$targetPath complete")
    }
  )
}
