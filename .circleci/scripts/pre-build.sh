#!/bin/bash
echo "PRE-BUILD Script invoked"
BASH_ENV=${BASH_ENV:-/tmp/bashrc}
rm -f ${BASH_ENV}

GIT_BRANCH=${CIRCLE_BRANCH:-$(git branch | grep \* | cut -d' ' -f2 )}
VERSION=$( grep '<version>' pom.xml | sed -e 's,^[^>]*>,,' -e 's,<.*$,,' -e 's,-[^-]*-SNAPSHOT$,,' -e 's,-SNAPSHOT$,,' -e 's,-testing$,,' -e 's,-,.,g' | head -n 1 )
RELEASE_BUILD_KEY="onms"
RELEASE_BRANCH=${GIT_BRANCH/\//-}
RELEASE_BUILD_NUM=${CIRCLE_BUILD_NUM:-1}
RELEASE_BUILDNAME=${RELEASE_BRANCH//[^[:alnum:]]/.}
RELEASE_MINOR_VERSION=$( git log --pretty="format:%cd" --date=short -1 | sed -e "s,^Date: *,," -e "s,-,,g" )
RELEASE_MICRO_VERSION="${RELEASE_BUILD_KEY}.${RELEASE_BUILDNAME}.${RELEASE_BUILD_NUM}"
MAVEN_DEFAULTS="-Xmx2048m -XX:ReservedCodeCacheSize=512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-UseGCOverheadLimit -XX:+UseParallelGC -XX:+UseParallelOldGC"
MAVEN_OPTS=\"${MAVEN_OPTS:-${MAVEN_DEFAULTS}}\"
echo "export OPENNMS_VERSION=${VERSION}" >> $BASH_ENV
echo "export RELEASE_MINOR_VERSION=${RELEASE_MINOR_VERSION}" >> $BASH_ENV
echo "export RELEASE_MICRO_VERSION=${RELEASE_MICRO_VERSION}" >> $BASH_ENV
echo "export INSTALL_VERSION=${VERSION}-0.${RELEASE_MINOR_VERSION}.${RELEASE_MICRO_VERSION}" >> $BASH_ENV
echo "export RELEASE_BRANCH=${RELEASE_BRANCH}" >> $BASH_ENV
echo "export GIT_BRANCH=${GIT_BRANCH}" >> $BASH_ENV
echo "export RELEASE_BUILD_NUM=${CIRCLE_BULD_NUM:-1}" >> $BASH_ENV
echo "export ARTIFACT_DIRECTORY=${ARTIFACT_DIRECTORY:-"~/artifacts"}" >> $BASH_ENV
echo "export MAVEN_OPTS=${MAVEN_OPTS}" >> $BASH_ENV

echo "Environment variables are auto detected and persisted in $BASH_ENV"
echo "So far I got:"
cat $BASH_ENV

source $BASH_ENV || exit

mvn --version
echo "MAVEN_OPTS: ${MAVEN_OPTS}"