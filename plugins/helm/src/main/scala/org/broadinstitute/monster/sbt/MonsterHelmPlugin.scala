package org.broadinstitute.monster.sbt

import io.circe.{Json, yaml}
import io.circe.yaml.syntax._
import sbt._
import sbt.Keys._
import sbt.nio.Keys._
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

  def packageChart(
    chartRoot: File,
    version: String,
    targetDir: File
  ): Unit = {
    val tmpDir = IO.createTemporaryDirectory
    IO.copyDirectory(chartRoot, tmpDir)
    // Wipe out the target/ directory, if it was copied over.
    IO.delete(tmpDir / "target")

    // Inject the version and appVersion so packaging does the right thing.
    val chartMetadata = chartRoot / "Chart.yaml"
    val parsedMetadata = yaml.parser.parse(IO.read(chartMetadata)) match {
      case Right(parsed) => parsed
      case Left(err)     => sys.error(s"Could not parse $chartMetadata as YAML: ${err.getMessage}")
    }
    val realVersion = Json.fromString(version)
    val updatedMetadata =
      parsedMetadata.deepMerge(Json.obj("version" -> realVersion, "appVersion" -> realVersion))
    IO.write(tmpDir / "Chart.yaml", updatedMetadata.asYaml.spaces2)

    // Use helm to package the staged template.
    // Assumes helm is available on the local PATH.
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
  }

  // Assumes chart-releaser is available on the local PATH,
  // and that CR_TOKEN is in the environment.
  def cr(cmd: String, args: (String, String)*): Unit = {
    val fullCommand =
      "cr" :: cmd :: args.toList.flatMap { case (k, v) => List(k, v) }
    val result = Process(fullCommand).!
    if (result != 0) {
      sys.error(s"`cr $cmd` failed with exit code $result")
    }
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    helmStagingDirectory := target.value / "helm",
    helmChartLocalIndex := helmStagingDirectory.value / "index.yaml",
    packageHelmChart / fileInputs += baseDirectory.value.toGlob / **,
    packageHelmChart := {
      val inputChanges = packageHelmChart.inputFileChanges
      val targetDir = target.value.toPath
      val filteredChanges = List
        .concat(
          inputChanges.created,
          inputChanges.modified,
          inputChanges.deleted
        )
        .filterNot(_.startsWith(targetDir))

      val packageTarget = helmStagingDirectory.value
      if (filteredChanges.nonEmpty) {
        packageChart(baseDirectory.value, version.value, packageTarget)
      }
      packageTarget
    },
    releaseHelmChart := {
      val log = streams.value.log
      val packageDir = packageHelmChart.value.getAbsolutePath()
      val org = helmChartOrganization.value
      val repo = helmChartRepository.value

      log.info(s"Uploading Helm chart for ${name.value} to $org/$repo...")
      cr("upload", "-o" -> org, "-r" -> repo, "-p" -> packageDir)
    },
    reindexHelmRepository := {
      val log = streams.value.log
      val packageDir = packageHelmChart.value.getAbsolutePath()
      val org = helmChartOrganization.value
      val repo = helmChartRepository.value
      val indexTarget = helmChartLocalIndex.value

      val indexSite = s"https://$org.github.io/$repo"
      log.info(s"Adding Helm chart for ${name.value} to index for $indexSite...")
      cr(
        "index",
        "-c" -> indexSite,
        "-o" -> org,
        "-r" -> repo,
        "-p" -> packageDir,
        "-i" -> indexTarget.getAbsolutePath()
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
        IO.copyFile(indexTarget, gitBase / "index.yaml")

        val diffCode = Process(Seq("git", "diff", "--quiet"), gitBase).!
        if (diffCode == 0) {
          log.info(s"Index for $indexSite is unchanged")
        } else {
          git("add", "index.yaml")
          git("commit", "-m", "Update index.")
          git("push", "-f", "origin", "gh-pages")
        }
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
