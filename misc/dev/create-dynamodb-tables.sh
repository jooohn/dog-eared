#!/bin/bash

aws dynamodb create-table \
  --endpoint-url ${AWS_DYNAMODB_ENDPOINT} \
  --table-name dog-eared-main \
  --attribute-definitions AttributeName=primaryKey,AttributeType=S AttributeName=sortKey,AttributeType=S AttributeName=data,AttributeType=S \
  --key-schema AttributeName=primaryKey,KeyType=HASH AttributeName=sortKey,KeyType=RANGE \
  --global-secondary-indexes '[
    {
      "IndexName":"sortKeyData",
      "KeySchema": [
        {
          "AttributeName": "sortKey",
          "KeyType": "HASH"
        },
        {
          "AttributeName": "data",
          "KeyType": "RANGE"
        }
      ],
      "Projection": {
        "ProjectionType": "KEYS_ONLY"
      },
      "ProvisionedThroughput": {
        "ReadCapacityUnits": 1,
        "WriteCapacityUnits": 1
      }
    },
    {
      "IndexName":"dataSortKey",
      "KeySchema": [
        {
          "AttributeName": "data",
          "KeyType": "HASH"
        },
        {
          "AttributeName": "sortKey",
          "KeyType": "RANGE"
        }
      ],
      "Projection": {
        "ProjectionType": "KEYS_ONLY"
      },
      "ProvisionedThroughput": {
        "ReadCapacityUnits": 1,
        "WriteCapacityUnits": 1
      }
    }
  ]' \
  --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1
