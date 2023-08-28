# File transfer between two different clouds

 This example shows how to exchange a data file between two EDC's using two different storage from cloud providers: IONOS and Azure.

 The consumer will use the IONOS S3 and the provider will use Azure Storage.

 It is based on [this](https://github.com/eclipse-edc/Samples/blob/main/transfer/transfer-05-file-transfer-cloud/README.md) EDC example and it will execute the connector locally.

## Requirements

You will need the following:
- IONOS account;
- Azure account;
- Azure CLI;
- Java Development Kit (JDK) 17 or higher;
- Terraform;
- Docker;
- GIT;
- Linux shell or PowerShell;

## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../../README.md).

### Configuration
In order to configure this example, please follow this steps:  
  
`Azure Storage`
- Execute the terraform script to create the required infrastructure for the provider connector;
```console
az login
cd terraform
terraform init --upgrade
terraform apply
```
- Copy the value fo the fields `client_id`, `tenant_id`, `vault-name`, `storage-container-name` and `storage-account-name`;
- Create a certificate file to authenticate against the Azure Active Directory:
```console
terraform output -raw certificate | base64 --decode > cert.pfx
```
- Edit the file `provider/resources`, which is the config properties for the provider, and put the terraform generated fields: 
```console
edc.vault.clientid=<client_id>
edc.vault.tenantid=<tenant_id>
edc.vault.certificate=<path_to_pfx_file>
edc.vault.name=<vault-name>
```
Note:
- the Azure vault will be created for the provider and the required data will be kept inside;
- the file `terraform/device1-data.csv` will be put into the `src-container` in the Azure storage; 

`IONOS S3`
- Create a token that the consumer will use to do the provisioning. Take a look at this [documentation](../../ionos_token.md);
- Put the token inside the Hashicorp vault instance (you can run it locally or in the IONOS Cloud). Take a look at this [documentation](../../hashicorp/README.md);
- Edit the file `consumer/resources`, which is the config properties for the consumer, and put the following fields: 
```console
edc.vault.hashicorp.url=<VAULT_ADDRESS:VAULT_PORT>
edc.vault.hashicorp.token=<ROOT_TOKEN>
edc.vault.hashicorp.timeout.seconds=30
```
Note: 
- the Hashicorp vault will have the required data for the consumer connector;

## Usage

Local execution:
```bash
java -Dedc.fs.config=example/file-transfer-multicloud/consumer/resources/consumer-config.properties -jar example/file-transfer-multicloud/consumer/build/libs/dataspace-connector.jar

java -Dedc.fs.config=example/file-transfer-multicloud/provider/resources/provider-config.properties -jar example/file-transfer-multicloud/provider/build/libs/dataspace-connector.jar
```

or

```bash
docker compose -f "docker-compose.yml" up --build
```
If you use docker to do the deployment of this example, don't forget to replace `localhost` with `consumer` and `provider` in the curls below.

We will have to call some URL's in order to transfer the file:
  
  (If you want, you can adapt and execute the `runDemo3.sh` script)

1) Asset creation for the consumer
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
             "properties": {
			 "type": "AzureStorage",
				"account": "edcionosstorage",
				"container": "src-container",
				"blobname": "device1-data.csv",
				"keyName" : "<storage-account-name>-key1"
            }
           }
         }'  -H 'X-API-Key: password' \
		 -H 'content-type: application/json' http://localhost:8182/management/v2/assets
```
Note: for the `account` and `keyName` fields use the output generated from the Terraform script;

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
		 -H 'content-type: application/json' http://localhost:8182/management/v2/policydefinitions
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
 -H 'content-type: application/json' http://localhost:8182/management/v2/contractdefinitions
```

4) Fetching the catalog
```console
curl -X POST "http://localhost:9192/management/v2/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
-d '{
      "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/"
      },
      "providerUrl": "http://localhost:8282/protocol",
      "protocol": "dataspace-protocol-http"
    }'
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

5) Contract negotiation
Copy the `policy{ @id` from the response of the first curl into this curl and execute it.

```
curl --location --request POST 'http://localhost:9192/management/v2/contractnegotiations' \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
	"@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "NegotiationInitiateRequestDto",
  "connectorId": "provider",
  "connectorAddress": "http://localhost:8282/protocol",
  "protocol": "dataspace-protocol-http",
  "offer": {
    "offerId": "1:1:a345ad85-c240-4195-b954-13841a6331a1",
    "assetId": "1",
    "policy": {"@id":<"REPLACE WHERE">,
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
}'
```

Note: copy the `id` field;

6) Contract agreement

Copy the value of the `id` from the response of the previous curl into this curl and execute it.
```console
curl -X GET "http://localhost:9192/management/v2/contractnegotiations/{<ID>}" \
	--header 'X-API-Key: password' \
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

Note: copy the `contractAgreementId` field;

7) Transfering the asset

Copy the value of the `contractAgreementId` from the response of the previous curl into this curl and execute it.
```console
curl -X POST "http://localhost:9192/management/v2/transferprocesses" \
    --header "Content-Type: application/json" \
	--header 'X-API-Key: password' \
    --data '{	
				"@context": {
					"edc": "https://w3id.org/edc/v0.0.1/ns/"
					},
				"@type": "TransferRequestDto",
                "connectorId": "consumer",
                "connectorAddress": "http://localhost:8282/protocol",
				"protocol": "dataspace-protocol-http",
                "contractId": "<CONTRACT AGREEMENT ID>",
                "assetId": "1",
				"dataDestination": { 
					"type": "IonosS3",
					"storage":"s3-eu-central-1.ionoscloud.com",
					"bucketName": "company2",
					"keyName" : "device1-data.csv"
				
				
				},
				"managedResources": false
        }'
```
Note: copy the `id` field to do the deprovisioning;

Accessing the bucket on the IONOS S3, you will see the `device1-data.csv` file.

8) Deprovisioning 

```
curl -X POST -H 'X-Api-Key: password' "http://localhost:9192/management/v2/transferprocesses/{<ID>}/deprovision"
```

Note: this will delete the IONOS S3 token from IONOS Cloud.
