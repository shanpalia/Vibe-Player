#!/bin/sh
set -e
GRADLE_VERSION=9.3.1
WRAPPER_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/vibe-player-gradle-$GRADLE_VERSION"
GRADLE_HOME="$WRAPPER_DIR/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$WRAPPER_DIR"
  ZIP="$WRAPPER_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    curl -fL --retry 3 --retry-delay 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  fi
  rm -rf "$GRADLE_HOME"
  mkdir -p "$GRADLE_HOME"
  TMP="$WRAPPER_DIR/.extract"
  rm -rf "$TMP" && mkdir -p "$TMP"
  unzip -q "$ZIP" -d "$TMP"
  mv "$TMP/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
  rm -rf "$TMP"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
