package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._
import sbtdynver.DynVerPlugin

/**
  * Plugin which should be applied to Monster sub-projects that need
  * to be published as library jars for use in other projects.
  */
object LibraryPlugin extends AutoPlugin {
  import DynVerPlugin.autoImport._

  override def requires: Plugins = BasePlugin

  /** Realm reported by our Artifactory instance. */
  private val artifactoryRealm = "Artifactory Realm"

  /** Hostname of our Artifactory instance. */
  private val artifactoryHost = "broadinstitute.jfrog.io"

  /** Environment variable expected to contain a username for our Artifactory. */
  private val artifactoryUsernameVar = "ARTIFACTORY_USERNAME"

  /** Environment variable expected to contain a password for our Artifactory. */
  private val artifactoryPasswordVar = "ARTIFACTORY_PASSWORD"

  /**
    * Credentials which can authenticate the build tool with our Artifactory.
    *
    * We frame this as a setting so that it is loaded once at the start of the build,
    * but within the proper DAG of settings set up by sbt.
    */
  private lazy val artifactoryCredentials = Def.setting {
    val cred = for {
      username <- sys.env.get(artifactoryUsernameVar)
      password <- sys.env.get(artifactoryPasswordVar)
    } yield {
      Credentials(artifactoryRealm, artifactoryHost, username, password)
    }

    cred.orElse {
      // SBT's logging comes from a task, and tasks can't be used inside settings, so we have to roll our own warning...
      println(
        s"[${scala.Console.YELLOW}warn${scala.Console.RESET}] $artifactoryUsernameVar or $artifactoryPasswordVar not set, publishing will fail!"
      )
      None
    }
  }

  /** Maven-style resolver for our Artifactory instance. */
  private lazy val artifactoryResolver = Def.task {
    val isPlugin = sbtPlugin.value
    val target = if (isSnapshot.value) "snapshot" else "release"
    val modulePattern = if (isPlugin) {
      "[module](_[scalaVersion])(_[sbtVersion])"
    } else {
      "[module](_[scalaVersion])"
    }
    val pattern =
      s"[organisation]/$modulePattern/[revision]/$modulePattern-[revision](-[classifier]).[ext]"

    Resolver.url(
      artifactoryRealm,
      new URL(s"https://$artifactoryHost/broadinstitute/libs-$target-local")
    )(Patterns().withArtifactPatterns(Vector(pattern)).withIsMavenCompatible(true))
  }

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    dynverSonatypeSnapshots := true
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    publishTo := Some(artifactoryResolver.value),
    credentials ++= artifactoryCredentials.value.toSeq
  )
}
