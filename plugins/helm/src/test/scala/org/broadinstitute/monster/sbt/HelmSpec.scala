package org.broadinstitute.monster.sbt

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sbt._

import scala.collection.mutable

class HelmSpec extends AnyFlatSpec with Matchers {
  behavior of "Helm"

  val sourceDir = new File("/source")
  val targetDir = new File("/target")
  val tmpDir = new File("/tmp")
  val version = "1.2.3-foo"
  val valuesFile = new File("/example/example1-values.yaml")

  it should "package charts" in {
    // Set up mocks.
    val chartYaml =
      s"""name: foo
         |version: 0.0.0
         |""".stripMargin
    val updatedChartYaml =
      s"""version: $version
         |appVersion: $version
         |name: foo
         |""".stripMargin
    val valuesYaml =
      s"""some: property
         |other:
         |  nested:
         |  - property
         |""".stripMargin

    val io = new HelmSpec.IO(
      Map(sourceDir / "Chart.yaml" -> chartYaml, sourceDir / "values.yaml" -> valuesYaml),
      tmpDir
    )
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    // Run the package command, ensuring we see the right value for version threaded through.
    helm.packageChart(sourceDir, version, targetDir, (json, v) => {
      v shouldBe version
      json
    })

    // Check that the expected I/O and helm commands were executed.
    io.dirCopies.toSet shouldBe Set(sourceDir -> tmpDir)
    io.deletes.toSet shouldBe Set(tmpDir / "target", targetDir)
    io.writes.toSet shouldBe Set(
      tmpDir / "Chart.yaml" -> updatedChartYaml,
      tmpDir / "values.yaml" -> valuesYaml
    )
    io.dirCreates.toSet shouldBe Set(targetDir)

    commands.toList shouldBe List(
      "dependency" -> List("update", "/tmp"),
      "package" -> List("/tmp", "--destination", "/target")
    )
  }

  it should "inject version into arbitrary values during packaging" in {
    // Set up mocks.
    val chartYaml =
      s"""name: foo
         |version: 0.0.0
         |""".stripMargin
    val updatedChartYaml =
      s"""version: $version
         |appVersion: $version
         |name: foo
         |""".stripMargin
    val valuesYaml =
      s"""some: property
         |other:
         |  nested:
         |  - property
         |""".stripMargin
    val updatedValuesYaml =
      s"""some: property
         |other:
         |  nested:
         |  - property
         |example-new-key:
         |  with-a-nested-property: $version
         |""".stripMargin

    val io = new HelmSpec.IO(
      Map(sourceDir / "Chart.yaml" -> chartYaml, sourceDir / "values.yaml" -> valuesYaml),
      tmpDir
    )
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    // Run the package command, ensuring we see the right value for version threaded through.
    helm.packageChart(
      sourceDir,
      version,
      targetDir,
      (json, v) => {
        v shouldBe version
        json.mapObject(
          _.add("example-new-key", Json.obj("with-a-nested-property" -> Json.fromString(version)))
        )
      }
    )

    // Check that the expected I/O and helm commands were executed.
    io.dirCopies.toSet shouldBe Set(sourceDir -> tmpDir)
    io.deletes.toSet shouldBe Set(tmpDir / "target", targetDir)
    io.writes.toSet shouldBe Set(
      tmpDir / "Chart.yaml" -> updatedChartYaml,
      tmpDir / "values.yaml" -> updatedValuesYaml
    )
    io.dirCreates.toSet shouldBe Set(targetDir)

    commands.toList shouldBe List(
      "dependency" -> List("update", "/tmp"),
      "package" -> List("/tmp", "--destination", "/target")
    )
  }

  it should "fail packaging if Chart.yaml is invalid YAML" in {
    val chartYaml = "blarg: glarg: glarb"
    val io = new HelmSpec.IO(Map(sourceDir / "Chart.yaml" -> chartYaml), tmpDir)
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    an[Exception] shouldBe thrownBy {
      // Run the package command, ensuring we see the right value for version threaded through.
      helm.packageChart(sourceDir, version, targetDir, (json, _) => json)
    }

    commands.toArray shouldBe empty
  }

  it should "fail packaging if values.yaml is invalid YAML" in {
    val chartYaml =
      s"""name: foo
         |version: 0.0.0
         |""".stripMargin
    val valuesYaml = "blarg: glarg: glarb"
    val io = new HelmSpec.IO(
      Map(sourceDir / "Chart.yaml" -> chartYaml, sourceDir / "values.yaml" -> valuesYaml),
      tmpDir
    )
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    an[Exception] shouldBe thrownBy {
      // Run the package command, ensuring we see the right value for version threaded through.
      helm.packageChart(sourceDir, version, targetDir, (json, _) => json)
    }

    commands.toArray shouldBe empty
  }

  it should "be resilient to missing values.yaml when injecting versions" in {
    // Set up mocks.
    val chartYaml =
      s"""name: foo
         |version: 0.0.0
         |""".stripMargin
    val updatedChartYaml =
      s"""version: $version
         |appVersion: $version
         |name: foo
         |""".stripMargin
    val updatedValuesYaml =
      s"""example-new-key:
         |  with-a-nested-property: $version
         |""".stripMargin

    val io = new HelmSpec.IO(
      Map(sourceDir / "Chart.yaml" -> chartYaml),
      tmpDir
    )
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    // Run the package command, ensuring we see the right value for version threaded through.
    helm.packageChart(
      sourceDir,
      version,
      targetDir,
      (json, v) => {
        v shouldBe version
        json.mapObject(
          _.add("example-new-key", Json.obj("with-a-nested-property" -> Json.fromString(version)))
        )
      }
    )

    // Check that the expected I/O and helm commands were executed.
    io.dirCopies.toSet shouldBe Set(sourceDir -> tmpDir)
    io.deletes.toSet shouldBe Set(tmpDir / "target", targetDir)
    io.writes.toSet shouldBe Set(
      tmpDir / "Chart.yaml" -> updatedChartYaml,
      tmpDir / "values.yaml" -> updatedValuesYaml
    )
    io.dirCreates.toSet shouldBe Set(targetDir)

    commands.toList shouldBe List(
      "dependency" -> List("update", "/tmp"),
      "package" -> List("/tmp", "--destination", "/target")
    )
  }

  it should "lint charts" in {
    val io = new HelmSpec.IO(Map.empty[File, String], tmpDir)
    val commands = new mutable.ArrayBuffer[(String, Seq[String])]()
    val helm = new Helm(io, (cmd, args) => commands.append(cmd -> args))

    helm.lintChart(sourceDir, valuesFile)

    io.dirCopies.toSet shouldBe Set(sourceDir -> tmpDir)
    io.deletes.toSet shouldBe Set(tmpDir / "target")

    commands.toList shouldBe List(
      "dependency" -> List("update", "/tmp"),
      "template" -> List("/tmp", "--values", "/example/example1-values.yaml", "--debug")
    )
  }
}

object HelmSpec {

  /** Mock utility for testing w/o touching the local file system. */
  class IO(fileContents: Map[File, String], tmpDir: File) extends Helm.IO {
    val writes = new mutable.ArrayBuffer[(File, String)]()
    val deletes = new mutable.ArrayBuffer[File]()
    val dirCreates = new mutable.ArrayBuffer[File]()
    val dirCopies = new mutable.ArrayBuffer[(File, File)]()

    override def readFile(f: File): String = fileContents(f)
    override def writeFile(f: File, s: String): Unit = writes.append(f -> s)
    override def deleteFile(f: File): Unit = deletes.append(f)
    override def fileExists(f: File): Boolean = fileContents.contains(f)
    override def createDirectory(f: File): Unit = dirCreates.append(f)
    override def createTempDirectory(): File = tmpDir
    override def copyDirectory(s: File, t: File): Unit = dirCopies.append(s -> t)
  }
}
