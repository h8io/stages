#!/bin/bash

set -euxo pipefail

sbt scalafmtSbtCheck scalafmtCheckAll +clean +coverage +test +doc +coverageSummary +coverageAggregate mdoc
