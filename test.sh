#!/bin/bash

set -euxo pipefail

sbt scalafmtSbtCheck scalafmtCheckAll \
    +clean +coverage +test +doc +packagedArtifacts \
    +coverageSummary +coverageAggregate \
    pages/tlSite
