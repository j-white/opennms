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

echo "#### Exporting Docker images..."
mkdir "${ARTIFACT_DIRECTORY}/docker"
for IMAGE in "opennms" "minion" "snmpd" "tomcat"
do
  docker image save stests/$IMAGE -o "${ARTIFACT_DIRECTORY}/docker/stests-$IMAGE-docker-image"
done
