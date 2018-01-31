#!/bin/bash

DIR=$(cd $(dirname "$0") && pwd ) 

# RPM
${DIR}/package/makerpm.sh -m ${RELEASE_MINOR_VERSION} -u ${RELEASE_MICRO_VERSION} || exit

# DEB
#${DIR}/package/makedeb.sh -m ${RELEASE_MINOR_VERSION} -u ${RELEASE_MICRO_VERSION} || exit

# Installer
#${DIR}/package/makeinstaller.sh -m ${RELEASE_MINOR_VERSION} -u ${RELEASE_MICRO_VERSION} || exit