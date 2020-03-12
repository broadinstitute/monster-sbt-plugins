# Core Plugins
The `sbt` plugins bundled in this project form a baseline of useful functionality
we want to apply across all of our projects. The overarching `monster-sbt-plugins`
project recursively depends on this project to build itself, so care should be
taken when adding new functionality to avoid breaking that setup.

## MonsterBasePlugin
The `MonsterBasePlugin` is the lowest-level plugin that all Monster projects should
enable. It:
1. Sets the Scala version and compiler flags
2. Installs best-practice `sbt` plugins from external groups, and configures them
   to match our preferences. These include:
   * `scalafmt`, for automatic code formatting
   * `buildinfo`, for injecting build-level data into application code
   * `dynver`, for linking `sbt`'s version calculations to git
3. Configures access to read artifacts published to Broad's Artifactory instance
4. Sets up the test / integration test runners

All other plugins in this project depend on `MonsterBasePlugin`. It's unlikely
you'll ever need to enable it explicitly.

## MonsterLibraryPlugin
The `MonsterLibraryPlugin` wires the `publish` task to push project JARs into Broad's
Artifactory instance. The choice of whether JARs should be published into the
"snapshot" or "release" repository is driven by git, via `dynver` settings.

NOTE: If an `sbt` project in a repo enables the `MonsterLibraryPlugin`, then all of
the other projects it depends on in that repo must also enable the plugin. Otherwise,
the project will be published with dangling references to Artifactory URLs where the
upstream projects should have been uploaded.

## MonsterDockerPlugin
The `MonsterDockerPlugin` wires the `publish` task to build and push projects as Docker
images into DSP's public GCR repository. Projects enabling this plugin must have a main
class, or else manually override the `dockerEntrypoint` setting.

By default, the images build by the plugin are based on Graal VM, with Java 8. Image
tags match the `version` reported by `sbt`, which itself is linked to git via `dynver`.
The plugin also configures the docker build to tag and push a "latest" alias, to help
with running projects in dev.
