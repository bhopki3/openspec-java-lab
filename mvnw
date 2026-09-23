#!/bin/sh
set -eu

MAVEN_VERSION="3.9.11"
MAVEN_BASE="${MAVEN_USER_HOME:-${HOME}/.m2}/wrapper/dists"
MAVEN_DIR="${MAVEN_BASE}/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="${MAVEN_DIR}/bin/mvn"

if [ ! -x "${MAVEN_BIN}" ]; then
    ARCHIVE="${TMPDIR:-/tmp}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    mkdir -p "${MAVEN_BASE}"
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o "${ARCHIVE}"
    tar -xzf "${ARCHIVE}" -C "${MAVEN_BASE}"
fi

exec "${MAVEN_BIN}" "$@"
