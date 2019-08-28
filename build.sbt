lazy val `monster-sbt-plugins` = project
  .in(file("."))
  .settings(
    sbtPlugin := true,
    addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0"),
    addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.4")
  )
