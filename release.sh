#!/bin/bash

set -euxo pipefail

# The site is built before ci-release so that a broken build of it costs nothing: publishing to Maven Central is the
# one step here that cannot be taken back, and everything that can still fail belongs in front of it.
sbt +clean +test \
    pages/clean +pages/unidoc pages/tlSite \
    ci-release

mkdir -p target/pages
cp -pr pages/target/docs/site/. target/pages
mkdir -p target/pages/api/scala-2.12
cp -pr pages/target/scala-2.12/unidoc/. target/pages/api/scala-2.12
mkdir -p target/pages/api/scala-2.13
cp -pr pages/target/scala-2.13/unidoc/. target/pages/api/scala-2.13
