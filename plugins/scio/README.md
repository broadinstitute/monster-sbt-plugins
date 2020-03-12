# Scio Plugin
The `MonsterScioPipelinePlugin` bundled in this project enables useful functionality
for writing transformation pipelines using [Scio](https://github.com/spotify/scio).
Specifically, it:
1. Adds a dependency on Monster's [library](https://github.com/broadinstitute/monster-scio-utils/) of Scio utilities
2. Enables Scio-specific compiler flags
3. Configures the `MonsterDockerPlugin`, so that `publish` produces Argo-friendly
   pipeline runners

This plugin should be enabled on every Monster project containing a Scio pipeline
that runs as part of data ingest.
