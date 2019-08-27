// Spookiness: Use this project as its own plugins.
// Lifted from here: https://github.com/scalacenter/sbt-release-early/blob/master/project/plugins.sbt
Compile / unmanagedSourceDirectories +=
  baseDirectory.value.getParentFile / "src" / "main" / "scala"
