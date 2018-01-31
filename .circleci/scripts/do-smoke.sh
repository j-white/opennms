#!/bin/sh -e

export OPENNMS_RPM_ROOT="/home/circleci/rpms"
mkdir -p $OPENNMS_RPM_ROOT
cd $OPENNMS_RPM_ROOT

export URL_PREFIX="http://yum.opennms.org/bleeding/common/opennms"
export RPM_VERSION="22.0.0-0.20180130.onms.develop.1291"
wget -4 $URL_PREFIX/opennms-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-core-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-minion-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-minion-container-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-minion-features-core-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-minion-features-default-$RPM_VERSION.noarch.rpm
wget -4 $URL_PREFIX/opennms-webapp-jetty-$RPM_VERSION.noarch.rpm

git clone https://github.com/OpenNMS/opennms-system-test-api.git ~/stest-api || exit 1
cd ~/stest-api/docker
# Debug
echo "#### Files in RPM root"
ls $OPENNMS_RPM_ROOT
echo "#### Determined release"
export RELEASE=$(basename $OPENNMS_RPM_ROOT/opennms-minion-features-core-*.noarch.rpm | awk -F'-' '{ print $5; }')
echo $RELEASE
./copy-rpms.sh
echo "#### OpenNMS RPMs"
ls opennms/rpms
echo "#### Minion RPMs"
ls minion/rpms

./build-docker-images.sh

cd ~/repo
mvn -DupdatePolicy=never -DskipTests=true -DskipITs=true -P'!checkstyle' -Psmoke --projects :smoke-test --also-make clean install || exit 1
cd ~/repo/smoke-test
mvn -N -Dorg.opennms.smoketest.docker=true -DskipITs=false -Dtest=AboutPageIT integration-test
