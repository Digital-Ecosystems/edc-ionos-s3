# File transfer using push method on Kubernetes

This example shows how to exchange a data file between two EDC's. It is based on a sample of the official EDC respository.

You can execute this example by using only one IONOS account (more for development purpose) or by using two IONOS accounts (similar to production purpose).

## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 11 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** You can use the terraform script in [General-des-development](https://github.com/Digital-Ecosystems/general-des-development) repository to deploy a kubernetes cluster on IONOS DCD.
- [DAPS](https://github.com/Digital-Ecosystems/general-des-development/tree/main/omejdn-daps)


## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../../README.md).

### Configuration
In order to configure this sample, please follow this steps:
(We will use the [DCD](https://dcd.ionos.com))
1) Create a Kubernetes cluster and deploy DAPS service. Follow the instructions from the [general-des-development](https://github.com/Digital-Ecosystems/general-des-development/tree/main/omejdn-daps).
2) Create a S3 Key Management: access the `Storage\Object Storage\S3 Key Management` option and generate a Key. Keep the key and the secret;
3) Create the required buckets: access the `Storage\Object Storage\S3 Web Console` option and create two buckets: company1 and company2;
4) Upload a file named `device1-data.csv` into the company1 bucket. You can use the `example/file-transfer-push-daps/device1-data.csv`;
5) Create a token that the consumer will use to do the provisioning. Take a look at this [documentation](../../ionos_token.md);
6) Copy the required configuration fields:

Consumer: open the `example/file-transfer-push-daps/consumer/values.yaml` and add the following fields:
- `ids.webhook.address` - The address of the IDS Webhook
- `edc.keystore` - The keystore from the DAPS server in base64 encoded format
- `edc.keystorePassword` - The password of the keystore
- `edc.oauth.tokenUrl` - The token url of the DAPS server
- `edc.oauth.clientId` - The client id from the DAPS server
- `edc.oauth.providerJwksUrl` - The jwks url of the DAPS server
- `image.repository` - The repository of the consumer image

Provider: open the `example/file-transfer-push-daps/provider/values.yaml` and insert the key - `edc.ionos.accessKey` and the secret - `edc.ionos.secretKey` (step 1);
- `edc.ionos.token` with the token generated in step 5;
- `ids.webhook.address` - The address of the IDS Webhook
- `edc.keystore` - The keystore from the DAPS server in base64 encoded format
- `edc.keystorePassword` - The password of the keystore
- `edc.oauth.tokenUrl` - The token url of the DAPS server
- `edc.oauth.clientId` - The client id from the DAPS server
- `edc.oauth.providerJwksUrl` - The jwks url of the DAPS server
- `image.repository` - The repository of the provider image

Note: by design, S3 technology allows only unique names for the buckets. You may find an error saying that the bucket name already exists.

## Usage

```bash
export TF_VAR_kubeconfig='path/to/kubeconfig'
export TF_VAR_s3_access_key='<YOUR-KEY>'
export TF_VAR_s3_secret_key='<YOUR-SECRET-KEY>'
export TF_VAR_s3_endpoint='<IONOS-S3-ENDPOINT>' # e.g. s3-eu-central-1.ionoscloud.com
export TF_VAR_s3_token='<IONOS-TOKEN>'

cd terraform
./deploy-services.sh

# Forward the consumer port to your local machine
kubectl port-forward -n edc-ionos-s3-consumer svc/edc-ionos-s3-consumer 8182:8182

# Set the provider address which is accessible from the consumer
export PROVIDER_ADDRESS=edc-ionos-s3-provider.edc-ionos-s3-provider.svc.cluster.local
```


We will have to call some URL's in order to transfer the file:
1) Contract offers
```bash
curl -X GET -H 'X-Api-Key: password' "http://localhost:8182/api/v1/management/catalog?providerUrl=http://$PROVIDER_ADDRESS:8282/api/v1/ids/data"

```

You will have an output like the following:

```bash
{"id":"default","contractOffers":[{"id":"1:d285c5a4-aa7a-4e18-9c82-66eded1cd933","policy":{"permissions":[{"edctype":"dataspaceconnector:permission","uid":null,"target":"1","action":{"type":"USE","includedIn":null,"constraint":null},"assignee":null,"assigner":null,"constraints":[],"duties":[]}],"prohibitions":[],"obligations":[],"extensibleProperties":{},"inheritsFrom":null,"assigner":null,"assignee":null,"target":"1","@type":{"@policytype":"set"}},"asset":{"id":"1","createdAt":1672284626506,"properties":{"asset:prop:byteSize":null,"asset:prop:id":"1","asset:prop:fileName":null}},"provider":"urn:connector:provider","consumer":"urn:connector:consumer","offerStart":null,"offerEnd":null,"contractStart":"2022-12-29T03:30:26.055Z","contractEnd":"2022-12-29T04:30:26.055Z"},{"id":"2:c3dfbd92-7df5-46f5-a547-420bfde301e9","policy":{"permissions":[{"edctype":"dataspaceconnector:permission","uid":null,"target":"2","action":{"type":"USE","includedIn":null,"constraint":null},"assignee":null,"assigner":null,"constraints":[],"duties":[]}],"prohibitions":[],"obligations":[],"extensibleProperties":{},"inheritsFrom":null,"assigner":null,"assignee":null,"target":"2","@type":{"@policytype":"set"}},"asset":{"id":"2","createdAt":1672284626513,"properties":{"asset:prop:byteSize":null,"asset:prop:id":"2","asset:prop:fileName":null}},"provider":"urn:connector:provider","consumer":"urn:connector:consumer","offerStart":null,"offerEnd":null,"contractStart":"2022-12-29T03:30:26.055Z","contractEnd":"2022-12-29T04:30:26.055Z"}]}
```

2) Contract negotiation

Copy the bracket of the `policy` from the response of the first curl into this curl and execute it.

```bash
curl --location --request POST "http://localhost:8182/api/v1/management/contractnegotiations" \
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
curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/management/contractnegotiations/{<ID>}"
```
You will have an answer like the following:
```bash
{"createdAt":1672280687517,"updatedAt":1672280688733,"contractAgreementId":"1:83fc5fb4-84a9-4764-beea-4ff5446f91a0","counterPartyAddress":"http://$PROVIDER_ADDRESS:8282/api/v1/ids/data","errorDetail":null,"id":"f80e1c17-810c-4ed5-b066-b286c189bb92","protocol":"ids-multipart","state":"CONFIRMED","type":"CONSUMER"}
```

4) File transfer

Copy the value of the `contractAgreementId` from the response of the previous curl into this curl and execute it.
```bash
curl --location --request POST "http://localhost:8182/api/v1/management/transferprocess" \
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
      "bucketName": "daiteap-test2",
      "accessKey": "00ff8c8b6eda8b2789ea",
      "secretKey": "SxYwd9Od9mIFH9QtDLtSwZ1dnd1W0XG7BE1h6Jda",
      "account": "daiteap-test2"
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