val circeVersion = "0.12.3"
val circeDerivationVersion = "0.12.0-M7"
val enumeratumVersion = "1.5.13"
val enumeratumCirceVersion = "1.5.22"
val scalaTestVersion = "3.1.0"

val commonSettings = Seq(
  sbtPlugin := true,
  // Maven-style publishing is unforgivingly broken for sbt plugins.
  // Override publishing to use ivy-style here.
  publishMavenStyle := false,
  publishTo := {
    val resolver = Resolver.url(
      MonsterLibraryPlugin.ArtifactoryRealm,
      new URL(MonsterLibraryPlugin.fullResolverPath(isSnapshot.value))
    )
    Some(resolver)
  }
)

lazy val `monster-sbt-plugins` = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(`sbt-plugins-core`, `sbt-plugins-jade`, `sbt-plugins-scio`)

/** 'Core' plugins for use across all Monster sbt projects. */
lazy val `sbt-plugins-core` = project
  .in(file("plugins/core"))
  .enablePlugins(MonsterLibraryPlugin)
  .settings(
    commonSettings,
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.4.0"),
    addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1"),
    addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")
  )

/** Plugins for Monster sbt projects that generate data for Jade datasets. */
lazy val `sbt-plugins-jade` = project
  .in(file("plugins/jade"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`sbt-plugins-core`)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-circe" % enumeratumCirceVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-derivation" % circeDerivationVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

/** Plugins for Monster sbt projects that contain Scio processing pipelines. */
lazy val `sbt-plugins-scio` = project
  .in(file("plugins/scio"))
  .enablePlugins(MonsterLibraryPlugin)
  .dependsOn(`sbt-plugins-core`)
  .settings(
    commonSettings,
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
  )
