package org.broadinstitute.monster.sbt

import sbt._
import sbt.Keys._
import sbtdynver.DynVerPlugin

/**
  * Plugin which should be applied to Monster sub-projects that need
  * to be published as library jars for use in other projects.
  */
object MonsterLibraryPlugin extends AutoPlugin {
  import DynVerPlugin.autoImport._

  override def requires: Plugins = MonsterBasePlugin

  /** Realm reported by our Artifactory instance. */
  val ArtifactoryRealm = "Artifactory Realm"

  /** Hostname of our Artifactory instance. */
  val ArtifactoryHost = "broadinstitute.jfrog.io"

  /** Environment variable expected to contain a username for our Artifactory. */
  val ArtifactoryUsernameVar = "ARTIFACTORY_USERNAME"

  /** Environment variable expected to contain a password for our Artifactory. */
  val ArtifactoryPasswordVar = "ARTIFACTORY_PASSWORD"

  def fullResolverPath(isSnapshot: Boolean): String = {
    val target = if (isSnapshot) "snapshot" else "release"
    s"https://$ArtifactoryHost/broadinstitute/libs-$target-local"
  }

  /**
    * Credentials which can authenticate the build tool with our Artifactory.
    *
    * We frame this as a setting so that it is loaded once at the start of the build,
    * but within the proper DAG of settings set up by sbt.
    */
  private lazy val artifactoryCredentials = Def.setting {
    val cred = for {
      username <- sys.env.get(ArtifactoryUsernameVar)
      password <- sys.env.get(ArtifactoryPasswordVar)
    } yield {
      Credentials(ArtifactoryRealm, ArtifactoryHost, username, password)
    }

    cred.orElse {
      // SBT's logging comes from a task, and tasks can't be used inside settings, so we have to roll our own warning...
      println(
        s"[${scala.Console.YELLOW}warn${scala.Console.RESET}] $ArtifactoryUsernameVar or $ArtifactoryPasswordVar not set, publishing will fail!"
      )
      None
    }
  }

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    dynverSonatypeSnapshots := true
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    publishTo := Some(ArtifactoryRealm at fullResolverPath(isSnapshot.value)),
    credentials ++= artifactoryCredentials.value.toSeq
  )
}
