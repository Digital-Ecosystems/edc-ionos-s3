# Simple deployment of 2 EDC's

This example shows how to do a simple deployment of two EDC's using a Terraform script and also how to do a file exchange between a Provider and a Consumer.

## Deployment

You will create 2 `folders` called `Consumer` and `Provider`, for each of them do the checkout of this repository and follow this [readme](../../deployment/README.md).

**Don't forget to create unique parameters for each connector.**

Example:  
`Consumer`
```bash
export TF_VAR_namespace="edc-consumer"
export TF_VAR_vaultname="vaultconsumer"
```
`Provider`
```bash
export TF_VAR_namespace="edc-provider"
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

# buckets
export CONSUMER_BUCKET=<CONSUMER BUCKET NAME>
export PROVIDER_BUCKET=<PROVIDER BUCKET NAME>

# healthcheck
curl http://$PROVIDER_IP:8181/api/check/health
curl http://$CONSUMER_IP:8181/api/check/health
```

### File exchange flow

1) Asset creation for the consumer
```console
curl --header 'X-API-Key: password' \
-d '{
			"@context": {
             "edc": "https://w3id.org/edc/v0.0.1/ns/"
           },
           "asset": {
             "@id": "assetId",
			 "properties": {
              
               "name": "product description",
               "contenttype": "application/json"
             }
           },
           "dataAddress": {
             "properties": {
              "type": "AzureStorage",
              "bucketName": "'$PROVIDER_BUCKET'",
              "container": "'$PROVIDER_BUCKET'",
              "blobName": "device1-data.csv",
              "storage": "s3-eu-central-1.ionoscloud.com",
              "keyName": "device1-data.csv",
              "type": "IonosS3"
             }
           }
         }' -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/assets \
         -s | jq
```

2) Policy creation
```console
curl -d '{
			"@context": {
				"edc": "https://w3id.org/edc/v0.0.1/ns/",
				"odrl": "http://www.w3.org/ns/odrl/2/"
			},
           "@id": "aPolicy",
           "policy": {
             "@type": "set",
             "odrl:permission": [],
             "odrl:prohibition": [],
             "odrl:obligation": []
           }
         }' -H 'X-API-Key: password' \
		 -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/policydefinitions
```

3) Contract creation
```console
curl -d '{
           "@context": {
             "edc": "https://w3id.org/edc/v0.0.1/ns/"
           },
           "@id": "1",
           "accessPolicyId": "aPolicy",
           "contractPolicyId": "aPolicy",
           "assetsSelector": []
         }' -H 'X-API-Key: password' \
 -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/contractdefinitions
```

4) Fetching the catalog
```console
curl -X POST "http://$CONSUMER_IP:8182/management/v2/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
-d @- <<-EOF
{
      "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/"
      },
      "providerUrl": "http://PROVIDER_IP:8282/protocol",
      "protocol": "dataspace-protocol-http"
    }
EOF
```

5) Contract negotiation
```console

    export JSON_PAYLOAD=$(curl --location --request POST 'http://$CONSUMER_ADDRESS:8182/management/v2/contractnegotiations' \
    --header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    --data-raw '{
      "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
      },
      "@type": "NegotiationInitiateRequestDto",
      "connectorId": "provider",
      "connectorAddress": "http://$PROVIDER_ADDRESS:8282/protocol",
      "protocol": "dataspace-protocol-http",
      "offer": {
        "offerId": "1:1:a345ad85-c240-4195-b954-13841a6331a1",
        "assetId": "1",
        "policy": {"@id":"$OFFER_POLICY",
          "@type": "odrl:Set",
          "odrl:permission": {
            "odrl:target": "1",
            "odrl:action": {
              "odrl:type": "USE"
            }
          },
          "odrl:prohibition": [],
          "odrl:obligation": [],
          "odrl:target": "1"}
      }
    }' -s | jq -r '.["@id"]')
```
```console    
ID=$(curl -s --header 'X-API-Key: password' -X POST -H 'content-type: application/json'  "http://$CONSUMER_IP:8182/management/v2/contractnegotiations/$CONTRACT_NEGOTIATION_ID" | jq -r '.contractAgreementId')
echo $ID
```

6) Contract agreement
```console
CONTRACT_AGREEMENT_ID=$(curl -X GET "http://$CONSUMER_IP:8182/api/v1/data/contractnegotiations/$ID" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.["edc:contractAgreementId"]')
echo $CONTRACT_AGREEMENT_ID
```

7) Transfering the asset
```console
curl -X POST "http://$CONSUMER_IP:8182/management/v2/transferprocesses" \
    --header "Content-Type: application/json" \
	  --header 'X-API-Key: password' \
    -d @- <<-EOF
    {	
				"@context": {
					"edc": "https://w3id.org/edc/v0.0.1/ns/"
					},
				"@type": "TransferRequestDto",
                "connectorId": "consumer",
                "connectorAddress": "http://$PROVIDER_IP:8282/protocol",
				"protocol": "dataspace-protocol-http",
        "contractId": "$CONTRACT_AGREEMENT_ID",
        "protocol": "ids-multipart",
        "assetId": "assetId",
        "dataDestination": { 
					"type": "IonosS3",
					"storage":"s3-eu-central-1.ionoscloud.com",
					"bucketName": "$CONSUMER_BUCKET",
					"keyName" : "device1-data.csv"
				
				},
				"managedResources": false
    }
EOF
```

After executing all the steps, we can now check the `<IONOS S3 Destination Bucket>` to see if the file has been correctly transfered.
