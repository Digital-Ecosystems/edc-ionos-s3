# File transfer using pull method 

 This example shows how to exchange a data file between two EDC's. It is based on a [sample](https://github.com/eclipse-edc/Samples/blob/main/transfer/transfer-06-consumer-pull-http/README.md) of the official EDC respository that uses the HTTP to pull the data.


## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 17 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;

## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../../README.md).

### Configuration
In order to configure this sample, please follow this steps:
(We will use the [DCD](https://dcd.ionos.com))
1) Create a S3 Key Management: access the `Storage\Object Storage\S3 Key Management` option and generate a Key. Keep the key and the secret;
2) Create the required buckets: access the `Storage\Object Storage\S3 Web Console` option and create two buckets: company1;
3) Upload a file named `device1-data.csv` into the company1 bucket. You can use the `example/file-transfer-pull/device1-data.csv`;
4) Open the `example/file-transfer-pull/provider/resources/provider-config.properties` file and insert the key and the secret (step 1);

Note: by design, S3 technology allows only unique names for the buckets. You may find an error saying that the bucket name already exists.

## Usage

Local execution:
```bash
java -Dedc.keystore=example/file-transfer-pull/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=example/file-transfer-pull/consumer/resources/consumer-vault.properties -Dedc.fs.config=example/file-transfer-pull/consumer/resources/consumer-config.properties -jar example/file-transfer-pull/consumer/build/libs/dataspace-connector.jar

java -Dedc.keystore=example/file-transfer-pull/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=example/file-transfer-pull/provider/resources/provider-vault.properties -Dedc.fs.config=example/file-transfer-pull/provider/resources/provider-config.properties -jar example/file-transfer-pull/provider/build/libs/dataspace-connector.jar

java -jar ./example/file-transfer-pull/backend-service/build/libs/http-pull-connector.jar
```

or

```bash
docker compose -f "docker-compose.yml" up --build
```
If you use docker to do the deployment of this example, don't forget to replace `localhost` with `consumer` and `provider` in the curls below.

We will have to call some URL's in order to transfer the file:
  
  (If you want, you can execute the `runDemo2.sh` script and jump to the step `10`)

1) Register the data planes for the provider
```console
curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-provider-dataplane",
   "url": "http://localhost:19192/control/transfer",
   "allowedSourceTypes": [  "HttpData", "IonosS3" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData" ],
    "properties": {
     "publicApiUrl": "http://localhost:19291/public/"
   }
 }' \
     -X POST "http://localhost:19193/management/instances"
```

2) Register the data planes for the consumer
```console
curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-consumer-dataplane",
   "url": "http://localhost:29192/control/transfer",
   "allowedSourceTypes": [ "HttpData" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData" ],
   "properties": {
     "publicApiUrl": "http://localhost:29291/public/"
   }
 }' \
     -X POST "http://localhost:29193/management/instances"
```

3) Asset creation for the consumer
```console
curl -d '{
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
            
               "name": "device1-data.csv",
			   "bucketName": "company1",
               "container": "company1",
               "blobName": "device1-data.csv",
               "storage": "s3-eu-central-1.ionoscloud.com",
               "type": "IonosS3"
             
           }
           }' -H 'content-type: application/json' http://localhost:19193/management/v2/assets \
         -s | jq 
```

4) Policy creation
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
         }' -H 'content-type: application/json' http://localhost:19193/management/v2/policydefinitions \
         -s | jq	
```

5) Contract creation
```console
curl -d '{
           "@context": {
             "edc": "https://w3id.org/edc/v0.0.1/ns/"
           },
           "@id": "1",
           "accessPolicyId": "aPolicy",
           "contractPolicyId": "aPolicy",
           "assetsSelector": []
         }' -H 'content-type: application/json' http://localhost:19193/management/v2/contractdefinitions \
         -s | jq
```

6) Fetching the catalog
```console
curl -X POST "http://localhost:29193/management/v2/catalog/request" \
    -H 'Content-Type: application/json' \
    -d '{
      "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/"
      },
      "providerUrl": "http://localhost:19194/protocol",
      "protocol": "dataspace-protocol-http"
    }' -s | jq	
```

You will have an output like the following:

```
{
	"@id": "51dde18d-dc81-41ed-b110-591fdcea753f",
	"@type": "dcat:Catalog",
	"dcat:dataset": {
		"@id": "6519fb05-c1f3-4a81-a4c5-93f5ab128a22",
		"@type": "dcat:Dataset",
		"odrl:hasPolicy": {
			"@id": "1:1:67e38ac2-26e0-40c0-9628-e864f4e260f7",
			"@type": "odrl:Set",
			"odrl:permission": {
				"odrl:target": "1",
				"odrl:action": {
					"odrl:type": "USE"
				}
			},
			"odrl:prohibition": [],
			"odrl:obligation": [],
			"odrl:target": "1"
		},
		"dcat:distribution": [],
		"edc:id": "1"
	},
	"dcat:service": {
		"@id": "80e665f9-85f1-4ede-b2b5-0df6ed2d5ee3",
		"@type": "dcat:DataService",
		"dct:terms": "connector",
		"dct:endpointUrl": "http://localhost:8282/protocol"
	},
	"edc:participantId": "provider",
	"@context": {
		"dct": "https://purl.org/dc/terms/",
		"edc": "https://w3id.org/edc/v0.0.1/ns/",
		"dcat": "https://www.w3.org/ns/dcat/",
		"odrl": "http://www.w3.org/ns/odrl/2/",
		"dspace": "https://w3id.org/dspace/v0.8/"
	}
}
```

7) Contract negotiation

Copy the `policy{ @id` from the response of the first curl into this curl and execute it.

```console
curl -d '{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "NegotiationInitiateRequestDto",
  "connectorId": "provider",
  "connectorAddress": "http://localhost:19194/protocol",
  "consumerId": "consumer",
  "providerId": "provider",
  "protocol": "dataspace-protocol-http",
  "offer": {
   "offerId": "1:assetId:0ed3140c-0927-4ffd-a225-ba92d894eafe",
   "assetId": "assetId",
   "policy": {
     "@id": "@id":"<"REPLACE HERE">",
     "@type": "Set",
     "odrl:permission": [],
     "odrl:prohibition": [],
     "odrl:obligation": [],
     "odrl:target": "assetId"
   }
  }
}' -X POST -H 'content-type: application/json' http://localhost:29193/management/v2/contractnegotiations \
 -s | jq -r '.["@id"]'
```

8) Contract agreement

Copy the value of the `id` from the response of the previous curl into this curl and execute it.
```console
curl -X GET "http://localhost:29193/api/v1/data/contractnegotiations/{<ID>}" \
    --header 'Content-Type: application/json' \
    -s | jq	
```

You will have an answer like the following:
```
{
	"@type": "edc:ContractNegotiationDto",
	"@id": "a88180b3-0d66-41b5-8376-c91d8253afcf",
	"edc:type": "CONSUMER",
	"edc:protocol": "dataspace-protocol-http",
	"edc:state": "FINALIZED",
	"edc:counterPartyAddress": "http://localhost:8282/protocol",
	"edc:callbackAddresses": [],
	"edc:contractAgreementId": "1:1:5c0a5d3c-69ea-4fb5-9d3d-e33ec280cde9",
	"@context": {
		"dct": "https://purl.org/dc/terms/",
		"edc": "https://w3id.org/edc/v0.0.1/ns/",
		"dcat": "https://www.w3.org/ns/dcat/",
		"odrl": "http://www.w3.org/ns/odrl/2/",
		"dspace": "https://w3id.org/dspace/v0.8/"
	}
}
```

9) Transfering the asset

Copy the value of the `contractAgreementId` from the response of the previous curl into this curl and execute it.

```console
curl -X POST "http://localhost:29193/management/v2/transferprocesses" \
    -H "Content-Type: application/json" \
    -d '{
        "@context": {
          "edc": "https://w3id.org/edc/v0.0.1/ns/"
        },
        "@type": "TransferRequestDto",
        "connectorId": "provider",
        "connectorAddress": "http://localhost:19194/protocol",
        "contractId": "<CONTRACT AGREEMENT ID>",
        "assetId": "assetId",
        "managedResources": false,
        "protocol": "dataspace-protocol-http",
        "dataDestination": { 
          "type": "HttpProxy" 
        }
    }' \
    -s | jq
```

10) Getting authcode
Go to the backend log and copy the authcode.
Example of an authcode:
```console
eyJhbGciOiJSUzI1NiJ9.eyJleHAiOjE2NzgxMjEyMjQsImRhZCI6IntcInByb3BlcnRpZXNcIjp7XCJhdXRoS2V5XCI6XCJBdXRob3JpemF0aW9uXCIsXCJiYXNlVXJsXCI6XCJodHRwOi8vbG9jYWxob3N0OjE5MjkxL3B1YmxpYy9cIixcImF1dGhDb2RlXCI6XCJleUpoYkdjaU9pSlNVekkxTmlKOS5leUpsZUhBaU9qRTJOemd4TWpFeU1qTXNJbVJoWkNJNkludGNJbkJ5YjNCbGNuUnBaWE5jSWpwN1hDSmlZWE5sVlhKc1hDSTZYQ0pvZEhSd2N6b3ZMMnB6YjI1d2JHRmpaV2h2YkdSbGNpNTBlWEJwWTI5a1pTNWpiMjB2ZFhObGNuTmNJaXhjSW01aGJXVmNJanBjSWxSbGMzUWdZWE56WlhSY0lpeGNJblI1Y0dWY0lqcGNJa2gwZEhCRVlYUmhYQ0o5ZlNJc0ltTnBaQ0k2SWpFNk9EQXdaREppWXpNdFpUVXpPQzAwTjJVMkxXRTVNbUV0TUdFeU4yUXdabU5rWVRoakluMC5aSUg4TEhoR2l4UC1WRzBMLXY1MmFXSzlUYy1uOGUyUEgxZlNCSXBNNEZzdE03ejZOX1dGbmhnTDhVTURjUDFrdnJwRFJiWUV6Q1VHb3BRNE5aaTBKNzl6eVNTZWRCWmFVVVlIa3ZhMFdNUXo4dUhKMG1Jd0xnN2xMM0lDc1N4cDNMZ09LSjhyeVpJcGhxWTAzSG5ZOG9JMnlzWFdoTjBUaE02Y0lKWEZwbXRZLXNxSUh2d1FMaWZ2Rm9qUzM1d3VIZS1RdU9UMThnWElWZHc3cVJLWTVPTmdSWGhYQm04MUtxZThLRjk4QUNHbEc3Tk9Ea2lrSmpNWXlqVDAyRGJrWXVRd3Z2bDQ3VjhKZFJwSW1IcHkzS08tX1owOGdyVE1rSDRPUnVVaW5xRGkwbXZKQ0ZzX3ZOUG1jMnMtOUVzWjlFUE9ZMTk4anpoeGlNOTFMWXd4aVFcIixcInByb3h5TWV0aG9kXCI6XCJ0cnVlXCIsXCJwcm94eVF1ZXJ5UGFyYW1zXCI6XCJ0cnVlXCIsXCJwcm94eUJvZHlcIjpcInRydWVcIixcInR5cGVcIjpcIkh0dHBEYXRhXCIsXCJwcm94eVBhdGhcIjpcInRydWVcIn19IiwiY2lkIjoiMTo4MDBkMmJjMy1lNTM4LTQ3ZTYtYTkyYS0wYTI3ZDBmY2RhOGMifQ.I_0TxNPwCMdRfvkf6IUHGXPvTIHKoTaKpVKI-fmwYGnPsq-SDmmtcSZ60jadglIthS2DngfHZOY4mGVnjtarOTnG6x4y8RpWxRBC5YZRcmiCClTVIuYvrEPtpPAWx0bd0YRNCtKD_Wj_5P3t8nb3s8Ovh5G8qhK3byHrcg-UGpOhH8V5_zUS_bxh_mpIX42ytFm_kPMe9NDF3bvv9nH-wed53rleTHmZfTVgnaRvv7BDw2Yt97EbT-8FxwLQzpV8q7-8Lk-GkDTacpEEFfM-LFWsfhMFkIIHedVPfUoPwPNmqjWMUbFAynVr6SLnGBM9RFkuO31a0v2KWNi7p0Cp5Q
```

11) Confirmation of the asset transfer
```console
curl --location --request GET 'http://localhost:29291/public/' \
--header 'Authorization: <AUTHCODE>'
```

You will see the data of the file `device1-data.csv`.
