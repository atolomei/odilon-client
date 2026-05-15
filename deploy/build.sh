#!/bin/bash

set -e

# ==========================================
# MOVE TO PROJECT ROOT
# ==========================================



SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

# ==========================================
# CONFIG
# ==========================================

GROUP_PATH="$SCRIPT_DIR/io/odilon"
ARTIFACT_ID="odilon-client"
VERSION="1.15.2"

TARGET_DIR="${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}"

# ==========================================
# CREATE DIRECTORY STRUCTURE
# ==========================================

echo "Creating target directory..."
mkdir -p "$TARGET_DIR"

# ==========================================
# BUILD MAVEN ARTIFACTS
# ==========================================

echo ""
echo "Running Maven build from:"
echo "$PROJECT_DIR"

mvn clean install \
    source:jar \
    javadoc:jar \
    net.nicoulaj.maven.plugins:checksum-maven-plugin:1.11:artifacts \
    -DskipTests

# ==========================================
# COPY ARTIFACTS
# ==========================================

echo ""
echo "Copying artifacts to:"
echo "$TARGET_DIR"

cp target/*.jar "$TARGET_DIR/" || true
cp target/*.md5 "$TARGET_DIR/" || true
cp target/*.sha1 "$TARGET_DIR/" || true

# ==========================================
# GENERATE POM
# ==========================================

echo ""
echo "Generating pom artifact..."

cp pom.xml "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom"

# SHA1
shasum -a 1 "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom" \
    | awk '{print $1}' \
    > "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom.sha1"

# MD5
md5 -q "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom" \
    > "$TARGET_DIR/${ARTIFACT_ID}-${VERSION}.pom.md5"

# ==========================================
# DONE
# ==========================================

echo ""
echo "Artifacts ready in:"
echo "$TARGET_DIR"

echo ""
echo "Generated files:"
ls -1 "$TARGET_DIR"
