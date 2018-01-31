#!/bin/sh
mvn -DupdatePolicy=never -DskipTests=true -DskipITs=true -P'!checkstyle' -Psmoke --projects :smoke-test --also-make clean install || exit 1

export RPM_VERSION="20.1.0-1"
mkdir ~/rpms
cd ~/rpms
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-core-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-minion-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-minion-container-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-minion-features-core-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-minion-features-default-$RPM_VERSION.noarch.rpm
wget -4 http://yum.opennms.org/obsolete/common/opennms/opennms-webapp-jetty-$RPM_VERSION.noarch.rpm
export OPENNMS_RPM_ROOT="~/rpms"

git clone https://github.com/OpenNMS/opennms-system-test-api.git ~/stest-api || exit 1
cd ~/stest-api/docker
./copy-rpms.sh
./build-docker-images.sh

cd ~/repo/smoke-test
mvn -N -Dorg.opennms.smoketest.docker=true -DskipITs=false -Dtest=AboutPageIT integration-test
