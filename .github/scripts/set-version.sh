#!/bin/sh
set -e

VERSION=$1
MAJOR=$(echo "$VERSION" | cut -d. -f1)
MINOR=$(echo "$VERSION" | cut -d. -f2)
PATCH=$(echo "$VERSION" | cut -d. -f3)
VERSION_CODE=$((MAJOR * 1000000 + MINOR * 1000 + PATCH))

sed -i "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" app/build.gradle.kts
sed -i 's/versionName = "[^"]*"/versionName = "'"$VERSION"'"/' app/build.gradle.kts

echo "Set versionName=$VERSION versionCode=$VERSION_CODE"
