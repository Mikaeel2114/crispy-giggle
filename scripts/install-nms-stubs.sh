#!/usr/bin/env bash
set -euo pipefail
echo "[BedwarsAC] Installing v1_8_R3 NMS compile-time stubs into local Maven repository..."
mvn -B -f nms-stubs/pom.xml clean install
echo "[BedwarsAC] NMS v1_8_R3 stubs installed successfully."
