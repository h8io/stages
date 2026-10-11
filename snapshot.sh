#!/bin/bash

set -euxo pipefail

sbt --server cleanFull +compile ci-release
