#!/bin/bash
mkdir -p ${ARTIFACT_DIRECTORY}
mkdir -p ${ARTIFACT_DIRECTORY}/rpms
# Screenshots taken by Selenium tests
mkdir -p ${ARTIFACT_DIRECTORY}/screenshots
mkdir -p ${ARTIFACT_DIRECTORY}/test-results/
mkdir -p ${ARTIFACT_DIRECTORY}/targz
mkdir -p ${ARTIFACT_DIRECTORY}/zip
find . -type f -name "*.rpm" -exec cp -v {} ${ARTIFACT_DIRECTORY}/rpms \;
find . -type f -regex ".*/target/screenshots/*" -exec cp -v {} ${ARTIFACT_DIRECTORY}/screenshots \;
find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ${ARTIFACT_DIRECTORY}/test-results/ \;
find . -type f -regex ".*/target/failsafe-reports/.*xml" -exec cp {} ${ARTIFACT_DIRECTORY}/test-results/ \;
find . -type f -name "*.tar.gz" -exec cp -v {} ${ARTIFACT_DIRECTORY}/targz \;
find . -type f -name "*.zip" -exec cp -v {} ${ARTIFACT_DIRECTORY}/zip \;
tar -czf ${ARTIFACT_DIRECTORY}/javadocs.tar.gz target/site/apidocs
