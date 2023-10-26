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
- [JQ Tool](https://jqlang.github.io/jq/);

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
curl --request POST --location 'http://localhost:8182/management/v2/assets' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
   },
   "asset":{
      "@id":"asset-1",
      "properties":{
         "name":"product 1",
         "contenttype":"application/json"
      }
   },
   "dataAddress":{
      "type": "AzureStorage",
      "account": "<storage-account-name>",
      "container": "src-container",
      "blobname": "device1-data.csv",
      "keyName" : "<storage-account-name>-key1"
   }
}' | jq
```
Note: for the `account` and `keyName` fields use the output generated from the Terraform script;

2) Policy creation
```console
curl --request POST --location 'http://localhost:8182/management/v2/policydefinitions' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/",
      "odrl":"http://www.w3.org/ns/odrl/2/"
   },
   "@id":"policy-1",
   "policy":{
      "@type":"set",
      "odrl:permission":[],
      "odrl:prohibition":[],
      "odrl:obligation":[]
   }
}' | jq
```

3) Contract creation
```console
curl --request POST --location 'http://localhost:8182/management/v2/contractdefinitions' \
--header 'X-API-Key: password' --header 'Content-Type: application/json' \
--data-raw '{
   "@context":{
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
   },
   "@id":"contract-1",
   "accessPolicyId":"policy-1",
   "contractPolicyId":"policy-1",
   "assetsSelector":[]
}' | jq
```

4) Fetching the catalog
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

You will have an answer like the following:
```json
{
    "@id": "f1e02a5c-f545-4a34-bb94-8dec852867f6",
    "@type": "dcat:Catalog",
    "dcat:dataset": {
        "@id": "asset-1",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": {
            "@id": "MQ==:MQ==:ODQ2ZjY0ZDQtYWJjYS00MzM5LWFiMTMtNjM4MzM3MTBmZjg0",
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
        "@id": "868b5f3b-b7b5-482f-b969-c4b76235ab59",
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

5) Contract negotiation

Copy the `odrl:hasPolicy{ @id` from the response of the first curl into this curl and execute it.
```console
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

6) Contract agreement

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

7) Transfering the asset

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
    "@type": "edc:IdResponse",
    "@id": "6ac325ba-b869-4306-a3b7-57f1ccd29862",
    "edc:createdAt": 1697549933752,
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
