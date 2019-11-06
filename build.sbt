lazy val `monster-sbt-plugins` = project
  .in(file("."))
  .enablePlugins(MonsterLibraryPlugin)
  .settings(
    sbtPlugin := true,
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.4.0"),
    addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1"),
    addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0"),
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
