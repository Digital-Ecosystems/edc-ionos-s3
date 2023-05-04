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
cp terraform/.env-example terraform/.env
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

## Transfer file

1) Contract offers

```bash
RESPONSE_POLICY=$(curl -s -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/api/v1/management/catalog?providerUrl=http://$PROVIDER_ADDRESS:8282/api/v1/ids/data" | jq '.contractOffers[].policy')
```

2) Contract negotiation

We use the value of `policy` from the previous request response to create a contract negotiation.

```bash
RESPONSE_ID=$(curl -s --location --request POST "http://$CONSUMER_ADDRESS:8182/api/v1/management/contractnegotiations" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
  "connectorId": "provider",
  "connectorAddress": "http://'$PROVIDER_ADDRESS':8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "offer": {
    "offerId": "1:3a75736e-001d-4364-8bd4-9888490edb58",
    "assetId": "1",
    "policy": '"$RESPONSE_POLICY"'
  }
}' | jq -r '.id')
```

3) Contact Agreement id

We use the value of the `id` from the previous request response to get the `contractAgreementId`.
```bash
RESPONSE_CONTRACT_AGREEMENT_ID=$(curl -s -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/api/v1/management/contractnegotiations/$RESPONSE_ID" | jq -r '.contractAgreementId')
```

4) File transfer

**Note:** This steps creates a temporary S3 key in Ionos. Make sure your account have enough quota for S3 keys.

We use the value of the `contractAgreementId` from the response to transfer the file.
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
  "contractId": "'$RESPONSE_CONTRACT_AGREEMENT_ID'",
  "dataDestination": {
    "properties": {
      "type": "IonosS3",
      "storage":"s3-eu-central-1.ionoscloud.com",
      "bucketName": "'$TF_VAR_consumer_bucketname'"
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
After executing all the steps, we can now check the consumer bucket of our IONOS S3 to see if the file has been correctly transfered.

## Cleanup

```bash
# Navigate to the terraform folder
cd terraform

# Destroy the services
./deploy-services.sh destroy
```

## References
[deploy-services.sh documentation](./terraform/deploy-services.md)