package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object MonsterBasePlugin extends AutoPlugin {

  // Automatically apply our base settings to every project.
  override def requires = JvmPlugin
  override def trigger = allRequirements

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
    )
  )

  val BetterMonadicForVersion = "0.3.1"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion),
    Compile / console / scalacOptions := (Compile / scalacOptions).value.filterNot(
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
