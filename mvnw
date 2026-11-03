#!/bin/sh
# Maven Wrapper for Linux/macOS: downloads Maven on first run, then runs it.
set -e
MAVEN_VERSION=3.9.11
WRAPPER_DIR="$HOME/.m2/wrapper"
MAVEN_HOME_DIR="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME_DIR/bin/mvn" ]; then
  echo "Downloading Maven $MAVEN_VERSION ..."
  mkdir -p "$WRAPPER_DIR"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  curl -fsSL "$URL" | tar -xz -C "$WRAPPER_DIR"
fi

exec "$MAVEN_HOME_DIR/bin/mvn" "$@"
