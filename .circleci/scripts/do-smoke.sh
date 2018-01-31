#!/bin/sh
mvn -DupdatePolicy=never -DskipTests=true -DskipITs=true -Psmoke --projects :smoke-test --also-make clean install || exit 1
cd smoke-test
mvn -N -Dorg.opennms.smoketest.docker=true -DskipITs=false -Dtest=AboutPageIT integration-test
