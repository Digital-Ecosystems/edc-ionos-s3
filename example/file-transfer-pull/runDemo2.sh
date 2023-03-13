#!/bin/bash

### PROVIDER

curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-provider-dataplane",
   "url": "http://localhost:19192/control/transfer",
   "allowedSourceTypes": [ "HttpData", "IonosS3" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData" ],
   "properties": {
     "publicApiUrl": "http://localhost:19291/public/"
   }
 }' -X POST "http://localhost:19193/api/v1/data/instances"

### CONSUMER
curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-consumer-dataplane",
   "url": "http://localhost:29192/control/transfer",
   "allowedSourceTypes": [ "HttpData", "IonosS3" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData", "IonosS3" ],
   "properties": {
     "publicApiUrl": "http://localhost:29291/public/"
   }
 }' -X POST "http://localhost:29193/api/v1/data/instances"


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
	       "storage":"s3-eu-central-1.ionoscloud.com",
               "container": "pullcompany2",
               "bucketName": "pullcompany2",
	       "blobName": "device1-data.csv",
	       "name": "device1-data.csv",
               "type": "IonosS3"
             }
           }
         }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/assets

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
         }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/policydefinitions


 curl -d '{
   "id": "1",
   "accessPolicyId": "aPolicy",
   "contractPolicyId": "aPolicy",
   "criteria": []
 }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/contractdefinitions
 
curl -X POST "http://localhost:29193/api/v1/data/catalog/request" \
--header 'Content-Type: application/json' \
--data-raw '{
  "providerUrl": "http://localhost:19194/api/v1/ids/data"
}'

contractId=`curl -d '{
           "connectorId": "http-pull-provider",
           "connectorAddress": "http://localhost:19194/api/v1/ids/data",
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
         }' -X POST -H 'content-type: application/json' http://localhost:29193/api/v1/data/contractnegotiations \
         -s | jq -r '.id'`

echo "PJC: "$contractId

sleep 4

curl -X GET "http://localhost:29193/api/v1/data/contractnegotiations/$contractId" \
    --header 'Content-Type: application/json' \
    -s | jq 

sleep 4

contractAgreementId=`curl -X GET "http://localhost:29193/api/v1/data/contractnegotiations/$contractId" \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId'`
#    -s | jq `

echo "PJC - contractAgreementId: "$contractAgreementId

### TRANSFER
#curl -H 'Content-Type: application/json' \
#     -d '{
#   "destination": {
#     "properties": {
#	"type": "IonosS3"
#  "storage":"s3-eu-central-2.ionoscloud.com",
#  "bucketName": "pjcbucket2",
#  "blobName": "device1-data.csv"
#     }
#   },
#   "source": {
#     "properties": {
#	"type": "HttpData"
#     }
#   } 
# }' -X POST "http://localhost:29193/api/v1/data/instances/select"

# url=http://s3-eu-central-1.ionoscloud.com/pjcbucket?location=, headers=Host: s3-eu-central-2.ionoscloud.com
#"dataDestination": { "type": "HttpProxy" }

sleep 4

curl -X POST "http://localhost:29193/api/v1/data/transferprocess" \
    --header "Content-Type: application/json" \
    --data '{
                "connectorId": "http-pull-provider",
                "connectorAddress": "http://localhost:19194/api/v1/ids/data",
                "contractId": "'"$contractAgreementId"'",
                "assetId": "assetId",
                "managedResources": "false",
		"dataDestination": { 
		"properties": {
		   "type": "HttpProxy"
	        }
	        }
            }' \
    -s | jq 


echo "DONE"

#authcode=`curl http://localhost:29193/api/v1/data/transferprocess/$transferid`

#curl --location --request GET 'http://localhost:29291/public/' \
#--header 'Authorization: '"$authcode"''

#curl -H 'Content-Type: application/json' \
#     -d '{
#   "destination": {
#     "properties": {
#	"type": "IonosS3",
#  "storage":"s3-eu-central-2.ionoscloud.com",
#  "bucketName": "pjcbucket2",
#  "blobName": "device1-data.csv"
#}
#   },
#   "source": {
#	"type": "HttpData"
#   } 
# }' -X POST "http://localhost:29193/api/v1/data/instances/select"
