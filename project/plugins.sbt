// Spookiness: Use this project as its own plugins.
// Lifted from here: https://github.com/scalacenter/sbt-release-early/blob/master/project/plugins.sbt
Compile / unmanagedSourceDirectories +=
  baseDirectory.value.getParentFile / "src" / "main" / "scala"

// Needed for the dog-fooding setup, apparently.
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.4")
