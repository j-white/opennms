#!/usr/bin/env bash

# Tests
echo "Testing"
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode || exit
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode --file opennms-full-assembly/pom.xml || exit
