#!/bin/bash

set -euxo pipefail

sbt scalafmtSbtCheck scalafmtCheckAll \
    +clean +coverage +test \
    +coverageSummary +coverageAggregate +coverageSummaryCheck \
    +doc +packagedArtifacts pages/clean +pages/unidoc pages/tlSite
