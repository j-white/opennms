#!/bin/bash

# Run clean to validate project
echo "Cleaning project to validate." # if severe problems this fails faster than compiling
mvn clean -DupdatePolicy=never || exit
mvn clean -DupdatePolicy=never --file opennms-full-assembly/pom.xml || exit

# Compile
echo "Compiling..."
MAVEN_FLAGS="-DupdatePolicy=never -DskipTests=true -DskipITs=true -T 1C"
mvn install $MAVEN_FLAGS -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks || exit
mvn install $MAVEN_FLAGS -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks -Pdefault --file opennms-full-assembly/pom.xml || exit
mvn install $MAVEN_FLAGS -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --non-recursive --file opennms-tools/pom.xml || exit
mvn install $MAVEN_FLAGS -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --file opennms-tools/centric-troubleticketer/pom.xml || exit
mvn install $MAVEN_FLAGS -Psmoke --projects :smoke-test || exit 1

# Javadoc
echo "Generating Javadoc"
mvn javadoc:aggregate -DupdatePolicy=never --batch-mode -Prun-expensive-tasks || exit

