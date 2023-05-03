# File transfer using push method on Kubernetes

This example shows how to exchange a data file between two EDC's. It is based on a sample of the official EDC respository.

The connectors are deployed on Kubernetes cluster and use IDS DAPS as Identity Provider.

You can execute this example by using only one IONOS account (more for development purpose) or by using two IONOS accounts (similar to production purpose).

## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 11 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** You can use the terraform script in [General-des-development](https://github.com/Digital-Ecosystems/general-des-development) repository to deploy a kubernetes cluster on IONOS DCD.
- [DAPS server](https://github.com/Digital-Ecosystems/general-des-development/tree/main/omejdn-daps) - **Note:** You can follow the instructions in the [General-des-development](https://github.com/Digital-Ecosystems/general-des-development/tree/main/omejdn-daps) repository to deploy a DAPS server on IONOS DCD.
- 3 public IPs
- DNS server and domain name


## Deployment

### Configuration
In order to configure this sample, please follow this steps:
(We will use the [DCD](https://dcd.ionos.com))
1) Create a Kubernetes cluster and deploy DAPS service. Follow the instructions from the [general-des-development](https://github.com/Digital-Ecosystems/general-des-development/tree/main/omejdn-daps).
2) Create a S3 Key Management: access the `Storage/Object Storage/S3 Key Management` option and generate a Key. Keep the key and the secret;
3) Create the required buckets: access the `Storage/Object Storage/S3 Web Console` option and create two buckets: one for the provider and another for the consumer;
4) Upload a file named `device1-data.csv` into the provider bucket. You can use the `example/file-transfer-push-daps/device1-data.csv`;
5) Create a token that the consumer will use to do the provisioning. Take a look at this [documentation](../../ionos_token.md);

Note: by design, S3 technology allows only unique names for the buckets. You may find an error saying that the bucket name already exists.

## Usage

Create `example/file-transfer-push-daps/terraform/.env` file from the `example/file-transfer-push-daps/terraform/.env-example` file.

```bash
cp terraform/.env-example /terraform/.env
```

Open `example/file-transfer-push-daps/terraform/.env` and set all the variables.

```bash
# Navigate to the terraform folder
cd terraform

# Load the environment variables
source .env

# Deploy the services
./deploy-services.sh
```

```bash
# Set the provider and consumer addresses
export PROVIDER_ADDRESS=$(kubectl get svc -n edc-ionos-s3-provider edc-ionos-s3-provider -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export CONSUMER_ADDRESS=$(kubectl get svc -n edc-ionos-s3-consumer edc-ionos-s3-consumer -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

```


We will have to call some URL's in order to transfer the file:
1) Contract offers
```bash
curl -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/api/v1/management/catalog?providerUrl=http://$PROVIDER_ADDRESS:8282/api/v1/ids/data"

```

You will have an output like the following:

```bash
{"id":"default","contractOffers":[{"id":"1:d285c5a4-aa7a-4e18-9c82-66eded1cd933","policy":{"permissions":[{"edctype":"dataspaceconnector:permission","uid":null,"target":"1","action":{"type":"USE","includedIn":null,"constraint":null},"assignee":null,"assigner":null,"constraints":[],"duties":[]}],"prohibitions":[],"obligations":[],"extensibleProperties":{},"inheritsFrom":null,"assigner":null,"assignee":null,"target":"1","@type":{"@policytype":"set"}},"asset":{"id":"1","createdAt":1672284626506,"properties":{"asset:prop:byteSize":null,"asset:prop:id":"1","asset:prop:fileName":null}},"provider":"urn:connector:provider","consumer":"urn:connector:consumer","offerStart":null,"offerEnd":null,"contractStart":"2022-12-29T03:30:26.055Z","contractEnd":"2022-12-29T04:30:26.055Z"},{"id":"2:c3dfbd92-7df5-46f5-a547-420bfde301e9","policy":{"permissions":[{"edctype":"dataspaceconnector:permission","uid":null,"target":"2","action":{"type":"USE","includedIn":null,"constraint":null},"assignee":null,"assigner":null,"constraints":[],"duties":[]}],"prohibitions":[],"obligations":[],"extensibleProperties":{},"inheritsFrom":null,"assigner":null,"assignee":null,"target":"2","@type":{"@policytype":"set"}},"asset":{"id":"2","createdAt":1672284626513,"properties":{"asset:prop:byteSize":null,"asset:prop:id":"2","asset:prop:fileName":null}},"provider":"urn:connector:provider","consumer":"urn:connector:consumer","offerStart":null,"offerEnd":null,"contractStart":"2022-12-29T03:30:26.055Z","contractEnd":"2022-12-29T04:30:26.055Z"}]}
```

2) Contract negotiation

Copy the bracket of the `policy` from the response of the first curl into this curl and execute it.

```bash
curl --location --request POST "http://$CONSUMER_ADDRESS:8182/api/v1/management/contractnegotiations" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
  "connectorId": "provider",
  "connectorAddress": "http://'$PROVIDER_ADDRESS':8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "offer": {
    "offerId": "1:3a75736e-001d-4364-8bd4-9888490edb58",
    "assetId": "1",
    "policy": <POLICY>
  }
}'
```

You will have an answer like the following:
```bash
{"createdAt":1672280687517,"id":"f80e1c17-810c-4ed5-b066-b286c189bb92"}
```

3) Contact Agreement id

Copy the value of the `id` from the response of the previous curl into this curl and execute it.
```bash
curl -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/api/v1/management/contractnegotiations/<ID>"
```
You will have an answer like the following:
```bash
{"createdAt":1672280687517,"updatedAt":1672280688733,"contractAgreementId":"1:83fc5fb4-84a9-4764-beea-4ff5446f91a0","counterPartyAddress":"http://$PROVIDER_ADDRESS:8282/api/v1/ids/data","errorDetail":null,"id":"f80e1c17-810c-4ed5-b066-b286c189bb92","protocol":"ids-multipart","state":"CONFIRMED","type":"CONSUMER"}
```

4) File transfer

Copy the value of the `contractAgreementId` from the response of the previous curl into this curl and execute it.
```bash
curl --location --request POST "http://$CONSUMER_ADDRESS:8182/api/v1/management/transferprocess" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '
{
  "connectorAddress": "http://'$PROVIDER_ADDRESS':8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "connectorId": "consumer",
  "assetId": "1",
  "contractId": "<CONTRACT AGREEMENT ID>",
  "dataDestination": {
    "properties": {
      "type": "IonosS3",
      "storage":"s3-eu-central-1.ionoscloud.com",
      "bucketName": "<CONSUMER_BUCKET_NAME>",
    },
    "type": "IonosS3"
  },
  "managedResources": true,
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  }
}'
```
You will have an answer like the following:
```bash
{"createdAt":1673349183568,"id":"25df5c64-77c9-4e5a-8e4f-aa06aa434408"}
```
After executing all the steps, we can now check the `company2` bucket of our IONOS S3 to see if the file has been correctly transfered.