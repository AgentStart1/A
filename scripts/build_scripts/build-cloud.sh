#!/bin/sh
set -e

mkdir -p deploy/build
./gradlew \
    cloud:server:distTar cloud:server:distZip \
    cloud:ws:distTar cloud:ws:distZip \
    cloud:cli:distTar cloud:cli:distZip \
    cloud:worker:distTar cloud:worker:distZip \
    cloud:filesystem-service:distTar \
    cloud:lucene-service:distTar

cp cloud/server/build/distributions/* deploy/build
cp cloud/ws/build/distributions/* deploy/build
cp cloud/cli/build/distributions/* deploy/build
cp cloud/worker/build/distributions/* deploy/build
cp cloud/filesystem-service/build/distributions/filesystem-service.tar deploy/build
cp cloud/lucene-service/build/distributions/lucene-service.tar deploy/build
