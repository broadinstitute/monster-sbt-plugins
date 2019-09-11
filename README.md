# monster-sbt-plugins
Build plugins used by the Monster team in DSP

## Motivation
Over time, the Scala community has accumulated a set of best-practice settings
for configuring `sbt` builds. On top of that, Monster team has built up an
opinionated set of configs we want to be applied to our builds. This repository
provides a centralized location to define & version these combined settings.

## Why plugins?
Many options exist for sharing common code across `sbt` projects. The
[`giter8`](http://www.foundweekends.org/giter8/) is `sbt`-specific, and allows
for template builds to be imported and "filled in" to create new projects.
GitHub also provides support for generic
[template repositories](https://github.blog/2019-06-06-generate-new-repositories-with-repository-templates/).


These alternatives are simpler to publish than `sbt` plugins and provide
the same value when creating new projects. However, once a project has been
created using either of these methods, any updates to shared build settings
must be done manually. `sbt` plugins provide the long-term value of being able
to bump a single version number to pull in the latest settings updates, without
needing to know exactly what the updates might be.

## Installing plugins
Add the following to a project's `project/plugins.sbt` to install Monster's `sbt` plugins:
```sbt
val patternBase =
  "org/broadinstitute/monster/[module](_[scalaVersion])(_[sbtVersion])/[revision]"

val publishPatterns = Patterns()
  .withIsMavenCompatible(false)
  .withIvyPatterns(Vector(s"$patternBase/ivy-[revision].xml"))
  .withArtifactPatterns(Vector(s"$patternBase/[module]-[revision](-[classifier]).[ext]"))

resolvers += Resolver.url(
  "Broad Artifactory",
  new URL("https://broadinstitute.jfrog.io/broadinstitute/libs-release/")
)(publishPatterns)

addSbtPlugin("org.broadinstitute.monster" % "monster-sbt-plugins" % "<version>")
```

Eventually we intend to publish a higher-level template repository containing this boilerplate.

## Available plugins
| Plugin Name | Auto-applied? | Description |
| ----------- | ------------- | ----------- |
| `BasePlugin` | yes | Core settings for compilation, formatting, versioning, and test coverage. |
| `LibraryPlugin` | no | Settings for publishing to Broad's Artifactory instance. |
