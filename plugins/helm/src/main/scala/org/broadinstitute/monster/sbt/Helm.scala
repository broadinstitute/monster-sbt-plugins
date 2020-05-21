package org.broadinstitute.monster.sbt

import io.circe.Json
import io.circe.yaml
import io.circe.yaml.syntax._
import sbt._
import scala.sys.process._

class Helm(io: Helm.IO, runCommand: (String, Seq[String]) => Unit) {

  def packageChart(
    chartRoot: File,
    version: String,
    targetDir: File,
    injectVersionValues: (Json, String) => Json
  ): Unit = {
    // Copy the target chart into a temporary working location.
    val tmpDir = io.createTempDirectory()
    io.copyDirectory(chartRoot, tmpDir)
    // Wipe out the target/ directory, if it was copied over.
    io.deleteFile(tmpDir / "target")

    // Inject the version into Chart.yaml.
    val chartMetadata = chartRoot / "Chart.yaml"
    val newMetadata = yaml.parser.parse(io.readFile(chartMetadata)) match {
      case Right(metadata) => metadata.deepMerge(chartVersions(version))
      case Left(err)       => sys.error(s"Could not parse $chartMetadata as YAML: ${err.getMessage()}")
    }
    io.writeFile(tmpDir / "Chart.yaml", newMetadata.asYaml.spaces2)

    // Inject the version into values.yaml.
    val chartValues = chartRoot / "values.yaml"
    val baseValues = if (io.fileExists(chartValues)) {
      yaml.parser.parse(io.readFile(chartValues)) match {
        case Right(values) => values
        case Left(err)     => sys.error(s"Could not parse $chartValues as YAML: ${err.getMessage()}")
      }
    } else {
      Json.obj()
    }
    io.writeFile(tmpDir / "values.yaml", injectVersionValues(baseValues, version).asYaml.spaces2)

    // Clear out anything at the target location.
    io.deleteFile(targetDir)
    io.createDirectory(targetDir)

    runCommand("dependency", List("update", tmpDir.getAbsolutePath))
    runCommand("package", List(tmpDir.getAbsolutePath, "--destination", targetDir.getAbsolutePath))
  }

  private def chartVersions(version: String): Json = {
    val jsonVersion = Json.fromString(version)
    Json.obj("version" -> jsonVersion, "appVersion" -> jsonVersion)
  }
}

object Helm {

  /**
    * Interface for utility class that can perform the file-system IO
    * required by our Helm operations.
    *
    * We introduce this abstraction to support unit-testing.
    */
  trait IO {
    def readFile(f: File): String
    def writeFile(f: File, s: String): Unit
    def deleteFile(f: File): Unit
    def fileExists(f: File): Boolean
    def createDirectory(f: File): Unit
    def createTempDirectory(): File
    def copyDirectory(source: File, target: File): Unit
  }

  /** IO instance which performs operations against the local file system. */
  private val fileSystemIO: IO = new IO {
    override def readFile(f: File) = IO.read(f)
    override def writeFile(f: File, s: String): Unit = IO.write(f, s)
    override def deleteFile(f: File): Unit = IO.delete(f)
    override def fileExists(f: sbt.File) = f.exists()
    override def createDirectory(f: File): Unit = IO.delete(f)
    override def createTempDirectory(): File = IO.createTemporaryDirectory
    override def copyDirectory(source: File, target: File): Unit = IO.copyDirectory(source, target)
  }

  /** Helm instance which runs commands by using a locally-installed CLP. */
  val clp: Helm = new Helm(fileSystemIO, (cmd, args) => {
    val fullCommand = "helm" :: cmd :: args.toList
    val result = Process(fullCommand).!
    if (result != 0) {
      sys.error(s"`helm $cmd` failed with exit code $result")
    }
  })
}
