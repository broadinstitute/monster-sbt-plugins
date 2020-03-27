// Spookiness: Use this project as its own plugins.
// Lifted from here: https://github.com/scalacenter/sbt-release-early/blob/master/project/plugins.sbt
Compile / unmanagedSourceDirectories +=
  baseDirectory.value.getParentFile / "plugins" / "core" / "src" / "main" / "scala"

// Needed for the dog-fooding setup, apparently.
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
