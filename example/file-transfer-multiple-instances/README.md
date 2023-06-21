# Simple deployment of 2 EDC's

This example shows how to do a simple deployment of two EDC's using a Terraform script and also how to do a file exchange between a Provider and a Consumer.

## Deployment

You will create 2 `folders` called `Consumer` and `Provider`, for each of them do the checkout of this repository and follow this [readme](../../deployment/README.md).

Don't forget to create unique parameters for each connector.

Example:  
`Consumer`
```bash
export TF_VAR_namespace="edc_consumer"
export TF_VAR_vaultname="vaultconsumer"
```
`Provider`
```bash
export TF_VAR_namespace="edc_provider"
export TF_VAR_vaultname="vaultprovider"
```

## Usage
We will transfer the `device1-data.csv` file of this directory. 

### Configure the external IPs
First, we need to open a `shell console` to execute the following instructions.

```bash
# external IPs
CONSUMER_IP=<IP ADDRESS OF THE CONSUMER CONNECTOR>
PROVIDER_IP=<IP ADDRESS OF THE PROVIDER CONNECTOR>

# healthcheck
curl http://$PROVIDER_IP:8181/api/check/health
curl http://$CONSUMER_IP:8181/api/check/health
```

### File exchange flow

1) Asset creation for the consumer
```console
curl --header 'X-API-Key: password' \
-d '{
           "asset": {
             "properties": {
               "asset:prop:id": "assetId",
               "asset:prop:name": "product description",
               "asset:prop:contenttype": "application/json"
             }
           },
           "dataAddress": {
             "properties": {
			   "bucketName": "<SOURCE IONOS S3 bucket>",
			   "container": "<SOURCE IONOS S3 bucket>",
               "blobName": "device1-data.csv",
               "storage": "s3-eu-central-1.ionoscloud.com",
               "keyName": "device1-data.csv",
               "type": "IonosS3"
             }
           }
         }' -H 'content-type: application/json' http://$PROVIDER_IP:8182/api/v1/data/assets \
         -s | jq
```

2) Policy creation
```console
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
		 -H 'content-type: application/json' http://$PROVIDER_IP:8182/api/v1/data/policydefinitions		
```

3) Contract creation
```console
curl -d '{
   "id": "1",
   "accessPolicyId": "aPolicy",
   "contractPolicyId": "aPolicy",
   "criteria": []
 }' -H 'X-API-Key: password' \
 -H 'content-type: application/json' http://$PROVIDER_IP:8182/api/v1/data/contractdefinitions
```

4) Fetching the catalog
```console
curl -X POST "http://$CONSUMER_IP:8182/api/v1/data/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
-d @- <<-EOF
{
  "providerUrl": "http://$PROVIDER_IP:8282/api/v1/ids/data"
}
EOF
```

5) Contract negotiation
```console
JSON_PAYLOAD=$(cat <<-EOF
{
    "connectorId": "multicloud-push-provider",
    "connectorAddress": "http://$PROVIDER_IP:8282/api/v1/ids/data",
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
}
EOF
)
ID=$(curl -s --header 'X-API-Key: password' -X POST -H 'content-type: application/json' -d "$JSON_PAYLOAD" "http://$CONSUMER_IP:8182/api/v1/data/contractnegotiations" | jq -r '.id')
echo $ID
```

6) Contract agreement
```console
CONTRACT_AGREEMENT_ID=$(curl -X GET "http://$CONSUMER_IP:8182/api/v1/data/contractnegotiations/$ID" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId')
echo $CONTRACT_AGREEMENT_ID
```

7) Transfering the asset
```console
curl -X POST "http://$CONSUMER_IP:8182/api/v1/data/transferprocess" \
    --header "Content-Type: application/json" \
	--header 'X-API-Key: password' \
    -d @- <<-EOF
    {
        "connectorId": "consumer",
        "connectorAddress": "http://$PROVIDER_IP:8282/api/v1/ids/data",
        "contractId": "$CONTRACT_AGREEMENT_ID",
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
                "bucketName": "bucket-provider-1",
                "container": "bucket-consumer-1"
            }
        }
    }
EOF
```

After executing all the steps, we can now check the `<IONOS S3 Destination Bucket>` to see if the file has been correctly transfered.
