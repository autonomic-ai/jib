#!/bin/bash -

###
# ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
# The Apache License, Version 2.0
# ——————————————————————————————————————————————————————————————————————————————
# Copyright (C) 2019 Autonomic, LLC - All rights reserved
# ——————————————————————————————————————————————————————————————————————————————
# Proprietary and confidential.
# 
# NOTICE:  All information contained herein is, and remains the property of
# Autonomic, LLC and its suppliers, if any.  The intellectual and technical
# concepts contained herein are proprietary to Autonomic, LLC and its suppliers
# and may be covered by U.S. and Foreign Patents, patents in process, and are
# protected by trade secret or copyright law. Dissemination of this information
# or reproduction of this material is strictly forbidden unless prior written
# permission is obtained from Autonomic, LLC.
# 
# Unauthorized copy of this file, via any medium is strictly prohibited.
# ______________________________________________________________________________
###
# Usage: ./scripts/prepare_release.sh <release version> [<post-release-version>]

set -e

EchoRed() {
  echo "$(tput setaf 1; tput bold)$1$(tput sgr0)"
}
EchoGreen() {
  echo "$(tput setaf 2; tput bold)$1$(tput sgr0)"
}

Die() {
  EchoRed "$1"
  exit 1
}

DieUsage() {
  Die "Usage: ./scripts/prepare_release.sh <release version> [<post-release-version>]"
}

# Usage: CheckVersion <version>
CheckVersion() {
  [[ $1 =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z]+)?$ ]] || Die "Version: $1 not in ###.###.###[-XXX] format."
}

# Usage: IncrementVersion <version>
IncrementVersion() {
  local version=$1
  local minorVersion=$(echo $version | sed 's/[0-9][0-9]*\.[0-9][0-9]*\.\([0-9][0-9]\)*/\1/')
  local nextMinorVersion=$((minorVersion+1))
  echo $version | sed "s/\([0-9][0-9]*\.[0-9][0-9]*\)\.[0-9][0-9]*/\1.$nextMinorVersion/"
}

[ $# -ne 1 ] && [ $# -ne 2 ] && DieUsage

EchoGreen '===== RELEASE SETUP SCRIPT ====='

VERSION=$1
CheckVersion ${VERSION}

if [ -n "$2" ]; then
  NEXT_VERSION=$2
  CheckVersion ${NEXT_VERSION}
else
  NEXT_VERSION=$(IncrementVersion $VERSION)
  CheckVersion ${NEXT_VERSION}
fi

if [[ $(git status -uno --porcelain) ]]; then
  Die 'There are uncommitted changes.'
fi

# Runs integration tests.
./mvnw -e -PintegrationTest verify

# Checks out a new branch for this version release (eg. 1.5.7).
BRANCH=maven_release_v${VERSION}
git checkout -b ${BRANCH}

# Updates the pom.xml with the version to release.
mvn versions:set versions:commit -DnewVersion=${VERSION}

# Tags a new commit for this release.
TAG=v${VERSION}-maven
git commit -am "preparing release ${VERSION}"
git tag ${TAG}

# Updates the pom.xml with the next snapshot version.
# For example, when releasing 1.5.7, the next snapshot version would be 1.5.8-SNAPSHOT.
NEXT_SNAPSHOT=${NEXT_VERSION}
if [[ "${NEXT_SNAPSHOT}" != *-SNAPSHOT ]]; then
  NEXT_SNAPSHOT=${NEXT_SNAPSHOT}-SNAPSHOT
fi
mvn versions:set versions:commit -DnewVersion=${NEXT_SNAPSHOT}

# Commits this next snapshot version.
git commit -am "${NEXT_SNAPSHOT}"

# Pushes the tag and release branch to Github.
git push origin ${BRANCH}
git push origin ${TAG}

# File a PR on Github for the new branch. Have someone LGTM it, which gives you permission to continue.
EchoGreen 'File a PR for the new release branch:'
echo https://github.com/GoogleContainerTools/jib/compare/${BRANCH}
