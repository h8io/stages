#!/bin/bash

set -euxo pipefail

sbt clean pages/clean +pages/unidoc pages/tlSite

mkdir -p target/pages
cp -pr pages/target/docs/site/. target/pages
mkdir -p target/pages/api/scala-2.12
cp -pr pages/target/scala-2.12/unidoc/. target/pages/api/scala-2.12
mkdir -p target/pages/api/scala-2.13
cp -pr pages/target/scala-2.13/unidoc/. target/pages/api/scala-2.13
