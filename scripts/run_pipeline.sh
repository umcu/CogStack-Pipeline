#!/bin/bash

# TODO: pass these as arguments
./wait_for_service.sh "postgres:5432" "--timeout=0" "--stdout"
./wait_for_service.sh "elasticsearch:9200" "--timeout=0" "--stdout"


java -DLOG_FILE_NAME=$LOG_FILE_NAME -DLOG_LEVEL=$LOG_LEVEL -DFILE_LOG_LEVEL=$FILE_LOG_LEVEL -jar ${1} ${2}