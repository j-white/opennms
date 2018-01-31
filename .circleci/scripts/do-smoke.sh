#!/bin/sh
rm -rf ~/.m2/repository/org/opennms
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode || exit 1
pushd smoke-test
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true || exit 1
popd
