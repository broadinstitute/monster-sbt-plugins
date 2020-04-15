# Helm Plugin
The `MonsterHelmPlugin` bundled in this project provides tasks for packaging,
uploading, and indexing Helm charts to repositories hosted by GitHub pages.
Most of its business logic is delegated to `helm` and `chart-releaser`, so if
you're working on a "pure" Helm chart you probably just want to use those tools.

If you're working on a Helm chart that calls other tools from within the same project,
this chart can provide value by:
1. Wiring the `publish` task to also publish the Helm chart, along with any JARs / containers
2. Adding some pre-processing logic to inject the git-based version of the whole
   project into chart metadata before publishing

In the future, we may extend this plugin to also `lint` / `template` charts
as part of the `test` task.

## Using the Plugin
The plugin's existing tasks are intended to be used from a GitHub action (though
they should also work fine locally). You'll need to configure git and install tools
for the `publish` task to work correctly. The minimal set of required `steps` is:
```yaml
steps:
  - uses: actions/checkout@v2
  # Required to check out the `gh-pages` branch.
  - name: Fetch full git history
    run: git fetch --prune --unshallow
  # Required to push updates to `gh-pages`.
  - name: Configure git
    run: |
      git config user.name "$GITHUB_ACTOR"
      git config user.email "$GITHUB_ACTOR@users.noreply.github.com"
  # Required for sbt to work
  - uses: olafurpg/setup-scala@v7
    with:
      java-version: graalvm@20.0.0
  # Install Helm and chart-releaser
  - uses: azure/setup-helm@v1
    with:
      version: 'latest'
  - uses: broadinstitute/setup-chart-releaser@v1
    with:
      version: 'latest'
  # Upload the chart, and reindex the repository
  - name: Publish
    run: sbt publish reindexHelmRepository
    env:
      CR_TOKEN: ${{ secrets.ChartReleaserToken }}
```

The `ChartReleaserToken` secret must contain a personal access token
with "repo" scope, to support uploading new releases.
