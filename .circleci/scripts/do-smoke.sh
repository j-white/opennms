#!/bin/sh -e

echo "#### Restoring Docker images..."
for IMAGE in "opennms" "minion" "snmpd" "tomcat"
do
  docker image load -i "${ARTIFACT_DIRECTORY}/docker/stests-$IMAGE-docker-image"
done

echo "#### Pulling other referenced images..."
echo "###### Pulling postgres image from public registry"
docker pull postgres:9.5.1

echo "###### Pulling kafka 0.10.1.0 with scala 2.11 image from public registry"
docker pull spotify/kafka@sha256:cf8f8f760b48a07fb99df24fab8201ec8b647634751e842b67103a25a388981b

echo "###### Pulling elasticsearch images from public registry"
docker pull elasticsearch:2-alpine
docker pull elasticsearch:5-alpine

echo "#### Building dependencies"
cd ~/repo
mvn -DupdatePolicy=never -DskipTests=true -DskipITs=true -P'!checkstyle' -Psmoke --projects :smoke-test --also-make clean install
echo "#### Executing tests"
cd ~/repo/smoke-test
# Iterate through the tests instead of running a single command, since I can't find a way to make the later stop
# after the first failure
for TEST_CLASS in $(python3 ~/.circleci/scripts/find-tests.py --use-class-names . | circleci tests split)
do
  echo "###### Testing: ${TEST_CLASS}"
  mvn -N -Dorg.opennms.smoketest.docker=true -DskipTests=false -DskipITs=false -Dit.test=$TEST_CLASS integration-test
done
