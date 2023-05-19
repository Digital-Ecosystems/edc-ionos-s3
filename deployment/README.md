***
# EDC Ionos S3 deployment on external kubernetes cluster

### Local Deployment
For local kubernetes installation please refer to the ```kind``` directory's [readme](kind/README.md).

***
### External Deployment
To deploy the EDC Ionos S3 connector to external kubernetes cluster on IONOS cloud, follow this readme below.

***


### Requirements
- [Helm](https://helm.sh/docs/intro/install/)
- [Terraform](https://developer.hashicorp.com/terraform/downloads)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** follow instructions in the [general-des-development
](https://github.com/Digital-Ecosystems/general-des-development) directory to deploy a IONOS kubernetes cluster
- S3 account

***

These are the services that are deployed:
- [Vault](https://www.vaultproject.io/)
- [EDC-Ionos-S3](https://github.com/Digital-Ecosystems/edc-ionos-s3)

***

## Configuration

Set environment variables

**Note:** You will need docker image of the EDC Ionos S3 connector pushed to a repository. If you don't have one, you can build it following the instructions in the [readme](/connector/README.md).

**Note:** To create the IONOS token please take a look at the following [documentation](/ionos_token.md).

```sh
# Required configuration
export TF_VAR_namespace='edc-ionos-s3'
export TF_VAR_kubeconfig='path to kubeconfig'
export TF_VAR_vaultname='vault'  # optional if only 1 connector per cluster

export TF_VAR_s3_access_key='' # S3 access key
export TF_VAR_s3_secret_key='' # S3 secret key
export TF_VAR_s3_endpoint='' # s3 endpoint (e.g. s3-eu-central-1.ionoscloud.com)
export TF_VAR_ionos_token='' # IONOS Cloud token
```

In case you want to configure this Connector without Hashicorp Vault, you need to also set the parameters below in the helm [values.yaml](deployment/helm/edc-ionos-s3/values.yaml):

```yaml
  ionos:
    endpoint: <YOUR-S3-ENDPOINT>
    accessKey: <YOUR-KEY>
    secretKey: <YOUR-SECRET-KEY>
    token: <IONOS-TOKEN>
```

They should be the same as the ones set in the environment variables. The **ionos.endpoint** is set to the default S3 location, but it can be changed to any other location.


If you don't want the Connector to be externally accessible, you need to set the following parameters in the helm [values.yaml](deployment/helm/edc-ionos-s3/values.yaml):

```yaml
  service:
    type: ClusterIP
```

This will allocate a public IP address to the Connector. You can then access it on the ports 8181, 8182, and 8282.

> Note  If you are deploying multiple EDC Connectors on the same Kubernetes cluster, make sure **TF_VAR_namespace** and **TF_VAR_vaultname** parameters are unique for each Connector.

***

## Deploy

All commands paths are relative to the current directory where this readme is located.

### 1. Install the EDC Ionos S3 services

To install the services run the script ```deploy-services.sh``` in ```terraform``` directory.

```sh
cd terraform
./deploy-services.sh
```

### 2. Vault keys
After the services are installed you will have ```vault-keys.json``` file containing the vault keys in ```terraform``` directory.

### 3. Destroy the services

```sh
cd terraform
./destroy-services.sh
```

### 4. Using the EDC Connector

This szenario transfers a single file between two EDC Connectors. The first Connector is the Provider and the second Connector is the Consumer.

##### Configure the external IPs
```bash
# external IPs
CONSUMER_IP=85.215.200.77
PROVIDER_IP=85.215.200.7

# healthcheck
curl http://$PROVIDER_IP:8181/api/check/health
curl http://$CONSUMER_IP:8181/api/check/health
```

##### File exchange flow

```bash
### Create asset in PROVIDER
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
			   "bucketName": "company1",
			   "container": "company1",
               "blobName": "device1-data.csv",
               "storage": "s3-eu-central-1.ionoscloud.com",
               "keyName": "device1-data.csv",
               "type": "IonosS3"
             }
           }
         }' -H 'content-type: application/json' http://$PROVIDER_IP:8182/api/v1/data/assets \
         -s | jq

### create policy in PROVIDER
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

### Create contract in PROVIDER
curl -d '{
   "id": "1",
   "accessPolicyId": "aPolicy",
   "contractPolicyId": "aPolicy",
   "criteria": []
 }' -H 'X-API-Key: password' \
 -H 'content-type: application/json' http://$PROVIDER_IP:8182/api/v1/data/contractdefinitions

### Fetch calalog in CONSUMER from the PROVIDER
curl -X POST "http://$CONSUMER_IP:8182/api/v1/data/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
-d @- <<-EOF
{
  "providerUrl": "http://$PROVIDER_IP:8282/api/v1/ids/data"
}
EOF


### Get ID of contractnegotiations from CONSUMER
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

### Get CONTRACT_AGREEMENT_ID from CONSUMER
CONTRACT_AGREEMENT_ID=$(curl -X GET "http://$CONSUMER_IP:8182/api/v1/data/contractnegotiations/$ID" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId')
echo $CONTRACT_AGREEMENT_ID

### Initiate transferprocess in CONSUMER
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
                "bucketName": "merlotedcconsumernewnameten"
            }
        }
    }
EOF
```