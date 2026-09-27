#!/bin/sh
APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
VERSION="8.10.2"
DIST="$HOME/.gradle/wrapper/dists/gradle-$VERSION-bin"
INSTALL="$DIST/gradle-$VERSION"
ZIP="$DIST/gradle.zip"

mkdir -p "$DIST"
if [ ! -x "$INSTALL/bin/gradle" ]; then
  if [ ! -f "$ZIP" ]; then
    curl -L --fail -o "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  fi
  unzip -q -o "$ZIP" -d "$DIST"
fi
exec "$INSTALL/bin/gradle" "$@"
