#!/bin/bash

aws dynamodb scan \
  --endpoint-url "${AWS_DYNAMODB_ENDPOINT}" \
  --table-name dog-eared-main \
