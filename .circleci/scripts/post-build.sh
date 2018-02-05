#!/bin/bash
mkdir -p ${ARTIFACT_DIRECTORY}
mkdir -p ${ARTIFACT_DIRECTORY}/rpms
# Screenshots taken by Selenium tests
mkdir -p ${ARTIFACT_DIRECTORY}/screenshots
# Smoke test output and overlays
mkdir -p ${ARTIFACT_DIRECTORY}/test-output
mkdir -p ${ARTIFACT_DIRECTORY}/test-results
mkdir -p ${ARTIFACT_DIRECTORY}/targz
mkdir -p ${ARTIFACT_DIRECTORY}/zip
find . -type f -name "*.rpm" -exec cp -v {} ${ARTIFACT_DIRECTORY}/rpms \;

# Smoke test output handling
find . -type f -regex ".*/target/screenshots/*" -exec cp -v {} ${ARTIFACT_DIRECTORY}/screenshots \;
find ~/repo/smoke-test/target/* -maxdepth 0 -name "*.log" -exec cp -v {} ${ARTIFACT_DIRECTORY}/test-output \;
if [ -d ~/repo/smoke-test/target/overlays ]; then
  cp -R ~/repo/smoke-test/target/overlays ${ARTIFACT_DIRECTORY}/test-output/
fi

find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ${ARTIFACT_DIRECTORY}/test-results/ \;
find . -type f -regex ".*/target/failsafe-reports/.*xml" -exec cp {} ${ARTIFACT_DIRECTORY}/test-results/ \;
find . -type f -name "*.tar.gz" -exec cp -v {} ${ARTIFACT_DIRECTORY}/targz \;
find . -type f -name "*.zip" -exec cp -v {} ${ARTIFACT_DIRECTORY}/zip \;
if [ -d target/site/apidocs ]; then
  tar -czf ${ARTIFACT_DIRECTORY}/javadocs.tar.gz target/site/apidocs
fi
