#!/bin/bash

curl -d '{
           "asset": {
             "properties": {
               "asset:prop:id": "assetId",
               "asset:prop:name": "product description",
               "asset:prop:contenttype": "application/json"
             }
           },
           "dataAddress": {
             "properties": {
			 "type": "AzureStorage",
				"account": "<storage-account-name>",
				"container": "src-container",
				"blobname": "device1-data.csv",
				"keyName" : "<storage-account-name>-key1"
             }
           }
         }'  -H 'X-API-Key: password' \
		 -H 'content-type: application/json' http://localhost:8182/api/v1/management/assets

curl -d '{
           "id": "aPolicy",
           "policy": {
             "uid": "231802-bb34-11ec-8422-0242ac120002",
             "permissions": [
               {
                 "target": "assetId",
                 "action": {
                   "type": "USE"
                 },
                 "edctype": "dataspaceconnector:permission"
               }
             ],
             "@type": {
               "@policytype": "set"
             }
           }
         }' -H 'X-API-Key: password' \
		 -H 'content-type: application/json' http://localhost:8182/api/v1/management/policydefinitions


 curl -d '{
   "id": "1",
   "accessPolicyId": "aPolicy",
   "contractPolicyId": "aPolicy",
   "criteria": []
 }' -H 'X-API-Key: password' \
 -H 'content-type: application/json' http://localhost:8182/api/v1/management/contractdefinitions
 
curl -X POST "http://localhost:9192/api/v1/management/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
  "providerUrl": "http://localhost:8282/api/v1/ids/data"
}'

contractId=`curl -d '{
           "connectorId": "multicloud-push-provider",
           "connectorAddress": "http://localhost:8282/api/v1/ids/data",
           "protocol": "ids-multipart",
           "offer": {
             "offerId": "1:50f75a7a-5f81-4764-b2f9-ac258c3628e2",
             "assetId": "assetId",
             "policy": {
               "uid": "231802-bb34-11ec-8422-0242ac120002",
               "permissions": [
                 {
                   "target": "assetId",
                   "action": {
                     "type": "USE"
                   },
                   "edctype": "dataspaceconnector:permission"
                 }
               ],
               "@type": {
                 "@policytype": "set"
               }
             }
           }
         }' --header 'X-API-Key: password' \
		 -X POST -H 'content-type: application/json' http://localhost:9192/api/v1/management/contractnegotiations \
         -s | jq -r '.id'`

echo "PJC: "$contractId


sleep 4

contractAgreementId=`curl -X GET "http://localhost:9192/api/v1/management/contractnegotiations/$contractId" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId'`
#    -s | jq `

echo "PJC - contractAgreementId: "$contractAgreementId



sleep 4

deprovisionId=`curl -X POST "http://localhost:9192/api/v1/management/transferprocess" \
    --header "Content-Type: application/json" \
	--header 'X-API-Key: password' \
    --data '{
                "connectorId": "consumer",
                "connectorAddress": "http://localhost:8282/api/v1/ids/data",
                "contractId": "'"$contractAgreementId"'",
				"protocol": "ids-multipart",
                "assetId": "assetId",
                "managedResources": "true",
				"transferType": {
					"contentType": "application/octet-stream",
					"isFinite": true
				  },
				"dataDestination": { 
				"properties": {
					"type": "IonosS3",
					"storage":"s3-eu-central-1.ionoscloud.com",
					"bucketName": "company2test"
						}
					}
        }' \
    -s | jq -r '.id'`
	
sleep 10
curl -X POST -H 'X-Api-Key: password' "http://localhost:9192/api/v1/management/transferprocess/$deprovisionId/deprovision"

echo "DONE"
