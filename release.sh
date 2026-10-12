#!/bin/bash

set -euxo pipefail

# The site is built before ci-release so that a broken build of it costs nothing: publishing to Maven Central is the
# one step here that cannot be taken back, and everything that can still fail belongs in front of it. That is also why
# sbt runs twice: Docusaurus is not part of the sbt build and has to fit in between.
sbt --server cleanFull pages/clean +testFull +pages/unidoc pages/mdoc
(cd pages && npm ci && npm run build)
sbt --server ci-release

mkdir -p target/pages
cp -pr pages/target/site/. target/pages
for version in 3 2.13 2.12; do
  mkdir -p target/pages/api/scala-$version
  cp -pr pages/target/scala-$version/unidoc/. target/pages/api/scala-$version
done
