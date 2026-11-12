#!/usr/bin/env bash
set -euo pipefail
chmod +x scripts/install-nms-stubs.sh
./scripts/install-nms-stubs.sh
mvn -B clean test package
echo "================================================================"
echo "Build complete! Plugin JAR: target/BedwarsAC-1.0.0-RELEASE.jar"
echo "================================================================"
