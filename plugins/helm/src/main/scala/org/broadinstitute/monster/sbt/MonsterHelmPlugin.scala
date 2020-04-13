package org.broadinstitute.monster.sbt

import io.circe.{Json, yaml}
import io.circe.yaml.syntax._
import sbt._
import sbt.Keys._
import scala.sys.process._

/**
  * Plugin for projects which wrap a Helm chart.
  *
  * Managing Helm in sbt is kinda a stretch. The main benefits are:
  *   1. Being able to inject the "official" version of the build into chart metadata
  *   2. Being able to run a single "publish" command and have charts get pushed
  *      along-side libraries and Docker images
  */
object MonsterHelmPlugin extends AutoPlugin {

  override def requires: Plugins = MonsterBasePlugin

  object autoImport extends MonsterHelmPluginKeys

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    helmStagingDirectory := target.value / "helm",
    helmChartLocalIndex := helmStagingDirectory.value / "index.yaml",
    packageHelmChart := {
      // Assumes the project is a valid Helm chart.
      val sourceDir = baseDirectory.value
      val tmpDir = IO.createTemporaryDirectory
      IO.copyDirectory(sourceDir, tmpDir)

      // Inject the version and appVersion so packaging does the right thing.
      val chartMetadata = sourceDir / "Chart.yaml"
      val parsedMetadata = yaml.parser.parse(IO.read(chartMetadata)) match {
        case Right(parsed) => parsed
        case Left(err)     => sys.error(s"Could not parse $chartMetadata as YAML: ${err.getMessage}")
      }
      val realVersion = Json.fromString(version.value)
      val updatedMetadata =
        parsedMetadata.deepMerge(Json.obj("version" -> realVersion, "appVersion" -> realVersion))
      IO.write(tmpDir / "Chart.yaml", updatedMetadata.asYaml.spaces2)

      // Make sure the target directory isn't included in the chart package.
      val ignores = sourceDir / ".helmignore"
      val defaultIgnores = if (ignores.exists) IO.readLines(ignores) else Nil
      val ignoresWithTarget = "target" :: defaultIgnores
      IO.writeLines(tmpDir / ".helmignore", ignoresWithTarget)

      // Use helm to package the staged template.
      // Assumes helm is available on the local PATH.
      val targetDir = helmStagingDirectory.value
      IO.delete(targetDir)
      IO.createDirectory(targetDir)

      val packageCommand = Seq(
        "helm",
        "package",
        tmpDir.getAbsolutePath(),
        "--destination",
        targetDir.getAbsolutePath()
      )
      val packageResult = Process(packageCommand).!
      if (packageResult != 0) {
        sys.error(s"`helm package` failed with exit code $packageResult")
      }
      targetDir
    },
    releaseHelmChart := {
      val log = streams.value.log
      val packageDir = packageHelmChart.value.getAbsolutePath()
      val org = helmChartOrganization.value
      val repo = helmChartRepository.value
      val indexTarget = helmChartLocalIndex.value

      // Assumes chart-releaser is available on the local PATH,
      // and that CR_TOKEN is in the environment.
      def cr(cmd: String, args: Map[String, String]): Seq[String] =
        "cr" :: cmd :: args.toList.flatMap { case (k, v) => List(k, v) }

      val uploadCommand =
        cr("upload", Map("-o" -> org, "-r" -> repo, "-p" -> packageDir))

      log.info(s"Uploading Helm chart for ${name.value} to $org/$repo...")
      val uploadResult = Process(uploadCommand).!
      if (uploadResult != 0) {
        sys.error(s"`cr upload` failed with exit code $uploadResult")
      }

      val indexSite = s"https://$org.github.io/$repo"
      val indexCommand = cr(
        "index",
        Map(
          "-c" -> indexSite,
          "-o" -> org,
          "-r" -> repo,
          "-p" -> packageDir,
          "-i" -> indexTarget.getAbsolutePath()
        )
      )

      log.info(s"Adding Helm chart for ${name.value} to index for $indexSite...")
      val indexResult = Process(indexCommand).!
      if (indexResult != 0) {
        sys.error(s"`cr index` failed with exit code $indexResult")
      }
    },
    publish := Def.taskDyn {
      if (isSnapshot.value) {
        // Don't bother publishing SNAPSHOT releases, since we can
        // always sync w/ Flux instead.
        Def.task {}
      } else {
        releaseHelmChart.toTask
      }
    }.value,
    // Doesn't make sense to publish a Helm chart locally(?)
    publishLocal := {}
  )
}
