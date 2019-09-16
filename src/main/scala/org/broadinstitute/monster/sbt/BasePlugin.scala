package org.broadinstitute.monster.sbt

import org.scalafmt.sbt.ScalafmtPlugin
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtbuildinfo.BuildInfoPlugin
import sbtdynver.DynVerPlugin
import scoverage.ScoverageSbtPlugin

/**
  * Plugin containing settings which should be applied to _every_ sbt project
  * managed by Monster, including:
  *   - Compiler flags
  *   - Auto-versioning
  *   - Code formatting
  *   - Test coverage generators
  *   - Build-info injection
  */
object BasePlugin extends AutoPlugin {
  import BuildInfoPlugin.autoImport._
  import DynVerPlugin.autoImport._
  import ScalafmtPlugin.autoImport._

  // Automatically apply our base settings to every project.
  override def requires: Plugins =
    JvmPlugin && DynVerPlugin && ScalafmtPlugin && ScoverageSbtPlugin && BuildInfoPlugin
  override def trigger = allRequirements

  val ScalafmtVersion = "2.0.1"

  val ScalafmtConf: String =
    s"""version = "$ScalafmtVersion"
       |maxColumn = 90
       |runner.optimizer.forceConfigStyleOnOffset = 90
       |
       |align = most
       |align.openParenCallSite = false
       |align.openParenDefnSite = false
       |binPack.literalArgumentLists = false
       |continuationIndent.callSite = 2
       |continuationIndent.defnSite = 2
       |danglingParentheses = true
       |
       |newlines.alwaysBeforeTopLevelStatements = true
       |newlines.sometimesBeforeColonInMethodReturnType = false
       |
       |includeCurlyBraceInSelectChains = false
       |""".stripMargin

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    organization := "org.broadinstitute.monster",
    scalaVersion := "2.12.10",
    scalacOptions ++= {
      val snapshot = isSnapshot.value
      val base = Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-explaintypes",
        "-feature",
        "-target:jvm-1.8",
        "-unchecked",
        "-Xfatal-warnings",
        "-Xfuture",
        "-Xlint",
        "-Xmax-classfile-name",
        "200",
        "-Yno-adapted-args",
        "-Ypartial-unification",
        "-Ywarn-dead-code",
        "-Ywarn-extra-implicit",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused",
        "-Ywarn-value-discard"
      )

      if (snapshot) {
        // -Xcheckinit adds extra synchronization logic / null checks to every
        // field access. It's awesome for catching problems with initialization
        // order when doing weird things with inheritance, but it can have
        // nontrivial performance impact, so we only enable it for SNAPSHOT builds.
        base :+ "-Xcheckinit"
      } else {
        base
      }
    },
    scalafmtConfig := {
      val targetFile = (ThisBuild / baseDirectory).value / ".scalafmt.conf"
      IO.write(targetFile, ScalafmtConf)
      targetFile
    },
    scalafmtOnCompile := true,
    // Use a Docker-compatible / URL-friendly separator for version components.
    dynverSeparator := "-"
  )

  val BetterMonadicForVersion = "0.3.1"

  override def projectConfigurations: Seq[Configuration] = Seq(IntegrationTest)

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq.concat(
      Defaults.itSettings,
      // Make sure integration-test sources are formatted, too.
      inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings),
      Seq(
        addCompilerPlugin(
          "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion
        ),
        Compile / console / scalacOptions := (Compile / scalacOptions).value
          .filterNot(
            Set(
              "-Xfatal-warnings",
              "-Xlint",
              "-Ywarn-unused",
              "-Ywarn-unused-import"
            )
          ),
        Compile / doc / scalacOptions += "-no-link-warnings",
        // Avoid classpath shenanigans by always forking a new JVM when running code.
        Runtime / fork := true,
        Test / fork := true,
        IntegrationTest / fork := true,
        // De-duplicate BuildInfo objects so our projects can depend on one another
        // without conflicts.
        buildInfoPackage := (ThisBuild / organization).value,
        buildInfoObject := name.value.split('-').map(_.capitalize).mkString + "BuildInfo",
        buildInfoOptions += BuildInfoOption.BuildTime
      )
    )
}
