#!/bin/bash
DIR=$(cd $(dirname "$0") && pwd )

source ${DIR}/pre-build.sh 
source ${DIR}/build.sh
source ${DIR}/test.sh
source ${DIR}/package.sh
source ${DIR}/post-build.sh