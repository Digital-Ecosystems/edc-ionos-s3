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
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** You can use the terraform script in [ionos-kubernetes-cluster](https://github.com/Digital-Ecosystems/ionos-kubernetes-cluster) repository to deploy a kubernetes cluster on IONOS DCD.
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

1. Fetch the catalog on consumer side

    In order to offer any data, the consumer can fetch the catalog from the provider, that will contain all the contract offers available for negotiation. In our case, it will contain a single contract offer. To get the catalog from the consumer side, you can use the following command:

    ```bash
    export OFFER_POLICY=$(curl -X POST "http://$CONSUMER_ADDRESS:8182/management/v2/catalog/request" \
    --header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -d '{
          "@context": {
            "edc": "https://w3id.org/edc/v0.0.1/ns/"
          },
          "providerUrl": "http://$PROVIDER_ADDRESS:8282/protocol",
          "protocol": "dataspace-protocol-http"
        }'-s | jq -r  '.["dcat:dataset"]["odrl:hasPolicy"]["@id"]')
    ```

    The offer policy is stored in the `OFFER_POLICY` variable.

    Sample output:
    ```json
    {
      "id": "default",
      "contractOffers": [
        {
          "id": "1:eade0787-41ce-4c72-bb9b-141c9671a14f",
          "policy": {
            "permissions": [
              {
                "edctype": "dataspaceconnector:permission",
                "uid": null,
                "target": "1",
                "action": {
                  "type": "USE",
                  "includedIn": null,
                  "constraint": null
                },
                "assignee": null,
                "assigner": null,
                "constraints": [],
                "duties": []
              }
            ],
            "prohibitions": [],
            "obligations": [],
            "extensibleProperties": {},
            "inheritsFrom": null,
            "assigner": null,
            "assignee": null,
            "target": "1",
            "@type": {
              "@policytype": "set"
            }
          },
          "asset": {
            "id": "1",
            "createdAt": 1683187807669,
            "properties": {
              "asset:prop:byteSize": null,
              "asset:prop:id": "1",
              "asset:prop:fileName": null
            }
          },
          "provider": "urn:connector:edc",
          "consumer": "urn:connector:edc",
          "offerStart": null,
          "offerEnd": null,
          "contractStart": null,
          "contractEnd": null
        }
      ]
    }
    ```

2. Negotiate a contract

    In order to request any data, a contract gets negotiated, and an agreement is resulting has to be negotiated between providers and consumers.

    The consumer now needs to initiate a contract negotiation sequence with the provider. That sequence looks as follows:

    Consumer sends a contract offer to the provider (currently, this has to be equal to the provider's offer!)
    Provider validates the received offer against its own offer
    Provider either sends an agreement or a rejection, depending on the validation result
    In case of successful validation, provider and consumer store the received agreement for later reference

    ```bash
    export CONTRACT_NEGOTIATION_ID=$(curl --location --request POST 'http://$CONSUMER_ADDRESS:8182/management/v2/contractnegotiations' \
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

    The contract negotiation id is stored in the `CONTRACT_NEGOTIATION_ID` variable.

    Sample output:
    ```json
    {
      "createdAt": 1674585892398,
      "id": "8ce50f33-25f3-42df-99e7-d6d72d83032c"
    }
    ```

3. Getting the contract agreement id

    After calling the endpoint for initiating a contract negotiation, we get a UUID as the response. This UUID is the ID of the ongoing contract negotiation between consumer and provider. The negotiation sequence between provider and consumer is executed asynchronously in the background by a state machine. Once both provider and consumer either reach the confirmed or the declined state, the negotiation is finished. We can now use the UUID to check the current status of the negotiation using an endpoint on the consumer side.

    ```bash
    export CONTRACT_AGREEMENT_ID=$(curl -s -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/management/v2/contractnegotiations/$CONTRACT_NEGOTIATION_ID" | jq -r '.contractAgreementId')
    ```

    The contract agreement id is stored in the `CONTRACT_AGREEMENT_ID` variable.

    Sample output:
    ```json
    {
      "createdAt": 1674585892398,
      "updatedAt": 1674585897476,
      "contractAgreementId": "1:307a028a-b2b3-495e-ab6c-f6dad24dd098",
      "counterPartyAddress": "http://provider:8282/api/v1/ids/data",
      "errorDetail": null,
      "id": "8ce50f33-25f3-42df-99e7-d6d72d83032c",
      "protocol": "ids-multipart",
      "state": "CONFIRMED",
      "type": "CONSUMER"
    }
    ```

4. Start the file transfer

    **Note:** This steps creates a temporary S3 key in Ionos. Make sure your account have enough quota for S3 keys.

    Now that we have a contract agreement, we can finally request the file. In the request body, we need to specify which asset we want transferred, the ID of the contract agreement, the address of the provider connector and where we want the file transferred. Execute the following command to start the file transfer:

    ```bash
    export TRAINSFER_PROCESSS_ID=$(curl -X POST "http://$CONSUMER_ADDRESS:8182/management/v2/transferprocesses" \
    --header "Content-Type: application/json" \
	--header 'X-API-Key: password' \
    --data '{	
				"@context": {
					"edc": "https://w3id.org/edc/v0.0.1/ns/"
					},
				"@type": "TransferRequestDto",
                "connectorId": "consumer",
                "connectorAddress": "http://$PROVIDER_ADDRESS:8282/protocol",
				"protocol": "dataspace-protocol-http",
                "contractId": "'$CONTRACT_AGREEMENT_ID'",
                "assetId": "1",
				"dataDestination": { 
					"type": "IonosS3",
					"storage":"s3-eu-central-1.ionoscloud.com",
					"bucketName": "'$TF_VAR_consumer_bucketname'",
					"keyName" : "device1-data.csv"
				
				
				},
				"managedResources": false
        }'
    
    
     | jq -r '.id')
    ```

    Then, we will get a UUID in the response. This time, this is the ID of the TransferProcess ( process id) created on the consumer side, because like the contract negotiation, the data transfer is handled in a state machine and performed asynchronously.

    You will have an answer like the following:
    ```bash
    {
        "createdAt": 1673349183568,
        "id": "25df5c64-77c9-4e5a-8e4f-aa06aa434408"
    }
    ```

5. Check the transfer status
    Due to the nature of the transfer, it will be very fast and most likely already done by the time you read the UUID.

    ```bash
    curl -X GET -H 'X-Api-Key: password' "http://$CONSUMER_ADDRESS:8182/api/v1/management/transferprocess/$TRAINSFER_PROCESSS_ID"
    ```


After executing all the steps, we can now check the consumer bucket of our IONOS S3 to see if the file has been correctly transfered.

## Troubleshooting
Get the logs from the connector pods

Provider
```bash
kubectl logs -n edc-ionos-s3-consumer deploy/edc-ionos-s3-consumer -f
```

Consumer
```bash
kubectl logs -n edc-ionos-s3-provider deploy/edc-ionos-s3-provider -f
```


## Cleanup

```bash
# Navigate to the terraform folder
cd terraform

# Destroy the services
./deploy-services.sh destroy
```

## References
[deploy-services.sh documentation](./terraform/deploy-services.md)