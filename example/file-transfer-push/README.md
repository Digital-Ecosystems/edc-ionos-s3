# File transfer using push method 

 This example shows how to exchange a data file between two EDC's. It is based on a sample of the official EDC respository.

 You can execute this example by using only one IONOS account (more for development purpose) or by using two IONOS accounts (similar to production purpose).

## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 17 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;
- [JQ Tool](https://jqlang.github.io/jq/); 

## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../../README.md).

### Configuration
In order to configure this sample, please follow this steps:
(We will use the [DCD](https://dcd.ionos.com))
1) Create a S3 Key Management: access the `Storage\Object Storage\S3 Key Management` option and generate a Key. Keep the key and the secret;
2) Create the required buckets: access the `Storage\Object Storage\S3 Web Console` option and create two buckets: company1;
3) Upload a file named `device1-data.csv` into the company1 bucket. You can use the `example/file-transfer-push/device1-data.csv`;
4) Create a token that the consumer will use to do the provisioning. Take a look at this [documentation](../../ionos_token.md);
5) Copy the required configuration fields:  
Consumer: open the `example/file-transfer-push/consumer/resources/consumer-config.properties` (or use an Hashicorp Vault instance) and add the field `edc.ionos.token` with the token;   
Provider: open the `example/file-transfer-push/provider/resources/provider-config.properties` (or use an Hashicorp Vault instance) and insert the key - `edc.ionos.access.key` and the secret - `edc.ionos.secret.access.key` (step 1);

Note: by design, S3 technology allows only unique names for the buckets. You may find an error saying that the bucket name already exists.


## Usage

Local execution:
```bash
java -Dedc.fs.config=example/file-transfer-push/consumer/resources/consumer-config.properties -jar example/file-transfer-push/consumer/build/libs/dataspace-connector.jar
java -Dedc.fs.config=example/file-transfer-push/provider/resources/provider-config.properties -jar example/file-transfer-push/provider/build/libs/dataspace-connector.jar
```

or

```bash
docker compose -f "docker-compose.yml" up --build
```
If you use docker to do the deployment of this example, don't forget to replace `localhost` with `consumer` and `provider` in the curls below.

We will have to call some URL's in order to transfer the file:
1) Contract offers
```console
curl --request POST --location 'http://localhost:9192/management/v2/catalog/request' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
   },
   "counterPartyAddress":"http://localhost:8282/protocol",
   "protocol":"dataspace-protocol-http"
}' | jq
```

You will have an output like the following:
```json
{
    "@id": "905f61f6-59ed-4e72-bec7-9301ca4c976f",
    "@type": "dcat:Catalog",
    "dcat:dataset": {
        "@id": "asset-1",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": {
            "@id": "Y29udHJhY3QtMQ==:YXNzZXQtMQ==:ZjMzNzk5YWItYzg0NS00ZjFhLWFmNGEtZTFiOGU4Y2FlY2I2",
            "@type": "odrl:Set",
            "odrl:permission": {
                "odrl:target": "asset-1",
                "odrl:action": {
                    "odrl:type": "USE"
                }
            },
            "odrl:prohibition": [],
            "odrl:obligation": [],
            "odrl:target": "asset-1"
        },
        "dcat:distribution": [],
        "edc:id": "asset-1"
    },
    "dcat:service": {
        "@id": "e83a9bda-9d73-462e-a06a-9424ea36a9a5",
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

2) Contract negotiation
Copy the `odrl:hasPolicy{ @id` from the response of the first curl into this curl and execute it.
```
curl --request POST --location 'http://localhost:9192/management/v2/contractnegotiations' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/",
      "odrl":"http://www.w3.org/ns/odrl/2/"
   },
   "@type":"NegotiationInitiateRequestDto",
   "connectorId":"provider",
   "connectorAddress":"http://localhost:8282/protocol",
   "consumerId":"consumer",
   "providerId":"provider",   
   "protocol":"dataspace-protocol-http",
   "offer":{
      "offerId":"<REPLACE_WHERE>",
      "assetId":"asset-1",
      "policy":{
         "@id":"<REPLACE_WHERE>",
         "@type":"odrl:Set",
         "odrl:permission": {
            "odrl:target": "asset-1",
            "odrl:action": {
               "odrl:type": "USE"
            }
         },
         "odrl:prohibition": [],
         "odrl:obligation": [],
         "odrl:target": "asset-1"
      }
   }
}' | jq
```

You will have an answer like the following:
```json
{
    "@type": "edc:IdResponse",
    "@id": "74774ea5-2757-4ea8-94cd-c33132bacd52",
    "edc:createdAt": 1697548933774,
    "@context": {
        "dct": "https://purl.org/dc/terms/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "dcat": "https://www.w3.org/ns/dcat/",
        "odrl": "http://www.w3.org/ns/odrl/2/",
        "dspace": "https://w3id.org/dspace/v0.8/"
    }
}
```

3) Contact Agreement id

Copy the value of the `@id` from the response of the previous curl into this curl and execute it.
```console
curl --request GET --location 'http://localhost:9192/management/v2/contractnegotiations/<REPLACE_WHERE>' --header 'X-API-Key: password' | jq
```

You will have an answer like the following:
```json
{
  "@type": "edc:ContractNegotiation",
  "@id": "74774ea5-2757-4ea8-94cd-c33132bacd52",
  "edc:type": "CONSUMER",
  "edc:protocol": "dataspace-protocol-http",
  "edc:state": "FINALIZED",
  "edc:counterPartyId": "provider",
  "edc:counterPartyAddress": "http://localhost:8282/protocol",
  "edc:callbackAddresses": [],
  "edc:createdAt": 1697548933774,
  "edc:contractAgreementId": "8f3ab27f-8279-433e-8218-843241302b59",
  "@context": {
    "dct": "https://purl.org/dc/terms/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "https://www.w3.org/ns/dcat/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/"
  }
}
```

4) File transfer

Copy the value of the `edc:contractAgreementId` from the response of the previous curl into this curl and execute it.
```console
curl --request POST --location 'http://localhost:9192/management/v2/transferprocesses' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
   },
   "@type":"TransferRequest",
   "connectorId":"consumer",
   "connectorAddress":"http://localhost:8282/protocol",
   "protocol":"dataspace-protocol-http",
   "contractId":"<REPLACE_WHERE>",
   "assetId":"asset-1",
   "dataDestination":{
      "type":"IonosS3",
      "storage":"s3-eu-central-1.ionoscloud.com",
      "bucketName":"company2",
      "blobName":"device1-data.csv",
      "keyName":"device1-data.csv"
   }
}' | jq
```

You will have an answer like the following:
```json
{
	"@type": "edc:IdResponseDto",
	"@id": "f9083e20-61a7-41c3-87f2-964de0ed2f52",
	"edc:createdAt": 1687364842252,
	"@context": {
		"dct": "https://purl.org/dc/terms/",
		"edc": "https://w3id.org/edc/v0.0.1/ns/",
		"dcat": "https://www.w3.org/ns/dcat/",
		"odrl": "http://www.w3.org/ns/odrl/2/",
		"dspace": "https://w3id.org/dspace/v0.8/"
	}
}
```

Note: Accessing the bucket on the IONOS S3, you will see the `device1-data.csv` file.

8) Deprovisioning

Copy the value of the `@id` from the response of the previous curl into this curl and execute it.
```console
curl --request POST --location 'http://localhost:9192/management/v2/transferprocesses/{<REPLACE_WHERE}/deprovision' --header 'X-API-Key: password'
```

Note: This will delete the IONOS S3 token from IONOS Cloud.
