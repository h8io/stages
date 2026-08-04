#!/bin/bash

set -euxo pipefail

sbt +clean +compile ci-release
