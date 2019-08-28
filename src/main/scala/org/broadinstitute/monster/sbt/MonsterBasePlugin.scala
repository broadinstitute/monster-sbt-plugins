package org.broadinstitute.monster.sbt

import org.scalafmt.sbt.ScalafmtPlugin
import sbt.{Def, _}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtdynver.DynVerPlugin

object MonsterBasePlugin extends AutoPlugin {
  import ScalafmtPlugin.autoImport._

  // Automatically apply our base settings to every project.
  override def requires = JvmPlugin && DynVerPlugin && ScalafmtPlugin
  override def trigger = allRequirements

  val ScalafmtVersion = "2.1.0-RC1"

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
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-feature",
      "-target:jvm-1.8",
      "-unchecked",
      "-Xcheckinit",
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
    ),
    scalafmtConfig := {
      val targetFile = (ThisBuild / baseDirectory).value / ".scalafmt.conf"
      IO.write(targetFile, ScalafmtConf)
      targetFile
    },
    scalafmtOnCompile := true
  )

  val BetterMonadicForVersion = "0.3.1"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
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
    Test / fork := true
  )
}
