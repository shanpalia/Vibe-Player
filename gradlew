#!/bin/sh
set -e
GRADLE_VERSION=9.3.1
WRAPPER_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/vibe-player-gradle-$GRADLE_VERSION"
GRADLE_HOME="$WRAPPER_DIR/gradle-$GRADLE_VERSION"
ZIP="$WRAPPER_DIR/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$WRAPPER_DIR"
  if [ ! -f "$ZIP" ] || [ ! -s "$ZIP" ]; then
    rm -f "$ZIP"
    echo "Downloading Gradle $GRADLE_VERSION..."
    curl -fL --retry 3 --retry-delay 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  fi
  rm -rf "$GRADLE_HOME"
  # The Gradle distribution already contains a top-level gradle-$GRADLE_VERSION directory.
  # Extract directly into the wrapper distribution directory; do not create a nested directory.
  unzip -q -o "$ZIP" -d "$WRAPPER_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
