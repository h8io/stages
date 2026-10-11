#!/bin/bash

set -euxo pipefail

# cleanFull also empties sbt's machine-wide disk cache. A compilation restored from it skips the compiler, and with it
# the scoverage plugin, so the instrumentation data that coverage measurements are written next to would be missing.
sbt --server scalafmtSbtCheck scalafmtCheckAll \
    cleanFull pages/clean +coverage +testFull \
    +coverageSummary +coverageAggregate +coverageSummaryCheck \
    +doc +packagedArtifacts +pages/unidoc pages/mdoc
(cd pages && npm ci && npm run build)
