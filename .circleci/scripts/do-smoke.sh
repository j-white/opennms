#!/bin/sh -e

echo "#### Building Docker images..."
git clone https://github.com/OpenNMS/opennms-system-test-api.git ~/stest-api
cd ~/stest-api/docker
export OPENNMS_RPM_ROOT="${ARTIFACT_DIRECTORY}/rpms"
echo "#### RPMs in ${OPENNMS_RPM_ROOT}:"
ls $OPENNMS_RPM_ROOT
./copy-rpms.sh
echo "#### OpenNMS RPMs:"
ls opennms/rpms
echo "#### Minion RPMs:"
ls minion/rpms
./build-docker-images.sh

cd ~/repo
mvn -DupdatePolicy=never -DskipTests=true -DskipITs=true -P'!checkstyle' -Psmoke --projects :smoke-test --also-make clean install
cd ~/repo/smoke-test
mvn -N -Dorg.opennms.smoketest.docker=true -DskipTests=false -DskipITs=false integration-test
