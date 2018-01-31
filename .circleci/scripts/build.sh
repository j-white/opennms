#!/bin/bash

# Run clean to validate project
echo "Cleaning project to validate." # if severe problems this fails faster than compiling
mvn clean -DupdatePolicy=never || exit
mvn clean -DupdatePolicy=never --file opennms-full-assembly/pom.xml || exit

# Compile
echo "Compiling..."
#  -Denable.snapshots=true
# ./compile.pl -DskipITs=true -Dmaven.test.skip.exec=true -Dinstall.version=22.0.0-0.20171214.onms.develop.1261 -Ddist.name=opennms-22.0.0-0.20171214.onms.develop.1261.x86_64 -Prun-expensive-tasks install
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks || exit
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks -Pdefault --file opennms-full-assembly/pom.xml || exit
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --non-recursive --file opennms-tools/pom.xml || exit
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --file opennms-tools/centric-troubleticketer/pom.xml || exit

# Javadoc
echo "Generating Javadoc"
mvn javadoc:aggregate -DupdatePolicy=never --batch-mode -Prun-expensive-tasks || exit

