#!/bin/bash
policyId=$(curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/management/catalog?providerUrl=http://localhost:8282/api/v1/ids/data" -s | jq -r '.contractOffers[].policy |tostring' )

sleep 2

conId=$(curl --location --request POST 'http://localhost:9192/api/v1/management/contractnegotiations' \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
  "connectorId": "provider",
  "connectorAddress": "http://localhost:8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "offer": {
    "offerId": "1:3a75736e-001d-4364-8bd4-9888490edb58",
    "assetId": "1",
    "policy": '$policyId'
  }
}' -s |jq -r '.id |tostring' )

echo "PL: "$conId
sleep 4
#echo $(curl -X GET -H 'X-Api-Key: password'"http://localhost:9192/api/v1/management/contractnegotiations/$conId" --header 'Content-Type: application/json')
	
contractAgId=$(curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/management/contractnegotiations/$conId" \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId |tostring')
echo "PL - contractAgreementId: "$contractAgId
sleep 2


curl --location --request POST 'http://localhost:9192/api/v1/management/transferprocess' \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '
{
  "connectorAddress": "http://localhost:8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "connectorId": "consumer",
  "assetId": "1",
  "contractId": "'"$contractAgId"'",
  "dataDestination": {
    "properties": {
      "type": "IonosS3",
 "storage":"s3-eu-central-1.ionoscloud.com",
      "bucketName": "company224"
    },
    "type": "IonosS3"
  },
  "managedResources": true,
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  }
}'


echo "DONE"
