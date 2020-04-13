package org.broadinstitute.monster.sbt

import io.circe.{Json, yaml}
import io.circe.yaml.syntax._
import sbt._
import sbt.Keys._
import scala.sys.process._
import scala.util.{Failure, Success, Try}

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
      def cr(cmd: String, args: Map[String, String]): Unit = {
        val fullCommand =
          "cr" :: cmd :: args.toList.flatMap { case (k, v) => List(k, v) }
        val result = Process(fullCommand).!
        if (result != 0) {
          sys.error(s"`cr $cmd` failed with exit code $result")
        }
      }

      log.info(s"Uploading Helm chart for ${name.value} to $org/$repo...")
      cr("upload", Map("-o" -> org, "-r" -> repo, "-p" -> packageDir))

      val indexSite = s"https://$org.github.io/$repo"
      log.info(s"Adding Helm chart for ${name.value} to index for $indexSite...")
      cr(
        "index",
        Map(
          "-c" -> indexSite,
          "-o" -> org,
          "-r" -> repo,
          "-p" -> packageDir,
          "-i" -> indexTarget.getAbsolutePath()
        )
      )

      val gitBase = (ThisBuild / baseDirectory).value

      def git(cmd: String, args: String*): Unit = {
        val fullCommand = Seq("git", cmd) ++ args
        val result = Process(fullCommand, gitBase).!
        if (result != 0) {
          sys.error(s"`git $cmd` failed with exit code $result")
        }
      }

      val currentBranch =
        Process(Seq("git", "rev-parse", "--abbrev-ref", "HEAD"), gitBase).!!.trim()

      log.info(s"Pushing updated index to $indexSite...")
      val attemptPushingIndex = Try {
        git("checkout", "gh-pages")
        IO.copy(List(indexTarget -> gitBase / "index.yaml"))
        git("add", "index.yaml")
        git("commit", "-m", s"Update index ($version)")
        git("push", "-f", "origin", "gh-pages")
      }

      attemptPushingIndex match {
        case Success(_) =>
          log.info(s"Successfully pushed index to $indexSite")
          git("checkout", currentBranch)
        case Failure(exception) =>
          git("checkout", currentBranch)
          sys.error(s"Failed to push index to $indexSite: ${exception.getMessage}")
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
