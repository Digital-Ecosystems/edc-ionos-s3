# IONOS S3 Extension for Eclipse Dataspace Connector

This repository contains the IONOS S3 Extension that works with the Eclipse Dataspace Connector allowing operations into the IONOS S3 Storage.

Disclaimer: The code of this repo is provided on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing this code and assume any risks associated with Your exercise of permissions. For more information check the License.

Please refer to the official [site](https://github.com/ionos-cloud/edc-ionos-s3).

## Based on the following

- [Eclipse EDC Connector](https://github.com/eclipse-edc/Connector) - v0.10.1;
- [International Data Spaces](https://www.internationaldataspaces.org);
- [GAIA-X](https://gaia-x.eu) project;

## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 17 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;

## Folders Description

### `extensions`
Contains the source code of the IONOS S3 Extension.

### `launchers`
Contains the required instructions to run an EDC Connector or create an EDC docker image with the IONOS S3 Extension.

## Dependencies and Configurations

### Dependencies
The extension has the following dependencies:

| Module name                                  | Description                                                      |
|----------------------------------------------|------------------------------------------------------------------|
| `extensions:provision-ionos-s3`              | Provisioning operations for IONOS S3 storage     |
| `extensions:data-plane-ionos-s3`             | Copy data do and from IONOS S3 buckets |
| `org.eclipse.edc:api-observability`          | Health data regarding the state of the connector |
| `org.eclipse.edc:auth-tokenbased`            | Securing the API |
| `org.eclipse.edc:api-control-plane-core`     | Main features of the control plane | 
| `org.eclipse.edc:configuration-filesystem`   | Configuration file features | 
| `org.eclipse.edc:http`                       | HTTP support | 
| `org.eclipse.edc:data-management-api`        | EDC asset and contract management |
| `org.eclipse.edc:data-plane-core`            | Main features of the data plane |
| `org.eclipse.edc:data-plane-selector-client` | Offers several implementations for the data plane selector |
| `org.eclipse.edc:data-plane-selector-core`   | Main features of the data plane selector |

### Configurations
It is required to configure an `Authentication Token` [docs](https://docs.ionos.com/cloud/set-up-ionos-cloud/management/token-management) and a `S3 Access key` [docs](https://docs.ionos.com/cloud/storage-and-backup/ionos-object-storage/concepts/key-management) to use the extension.

The credentials can be found/configured in one of the following:
- Vault;
- Properties file;
- Java arguments;
- Environment Variables (`IONOS_ACCESS_KEY`, `IONOS_SECRET_KEY` and `IONOS_TOKEN`);

It is required to configure those parameters:

| Parameter name                      | Description                                                                                                                                                       | Mandatory                                            |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| `edc.ionos.access.key`              | IONOS Access Key Id to access S3                                                                                                                                  | Yes if the context is accessing file                 |
| `edc.ionos.secret.access.key`       | IONOS Secret Access Key to access S3                                                                                                                              | Yes if the context is accessing file                 |
| `edc.ionos.token`                   | IONOS Token to allow S3 provisioning                                                                                                                              | Yes if the context is provisioning access for others |
| `edc.ionos.endpoint.region`         | IONOS S3 endpoint default region. It will be used if a region is not defined in the dataAddress or dataDestination. Refer to [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/endpoints) for further information. | No, the default value is "de"                        |
| `edc.ionos.max.files`               | Maximum number of files copied by S3 bucket folder.                                                                                                               | No, the default value is 5,000 files                 |
| `edc.ionos.key.validation.attempts` | Maximum number of attemps to validate a temporary key after its creation.                                                                                         | No, the default values is 10 attempts                |
| `edc.ionos.key.validation.delay`    | Time to wait (in milisseconds) before each key validation attempt. In each new attempt the delay is multiplied by the attempt number.                             | No, the default value is 3,000 (3 seconds)           |

## Building and Running

```bash
git clone git@github.com:ionos-cloud/edc-ionos-s3.git
cd extensions
./gradlew clean build
```

To run a connector with memory persistence:

```bash
cd launchers/prod/connector
java -Dedc.fs.config=resources/config.properties -jar build/libs/dataspace-connector.jar
```

To run a connector with database persistence:

```bash
cd launchers/prod/connector-persistence
java -Dedc.fs.config=resources/config.properties -jar build/libs/dataspace-connector.jar
```

## Deploying
Check the [deployment readme](https://github.com/ionos-cloud/edc-ionos-s3/tree/main/deployment/README.md) to see how to deploy the Connector locally or to an external Kubernetes cluster.

## Usage

### DataAddress
To create an asset using an IONOS S3 Bucket as its data address, use the following format:

To share a file:
```
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/")
    },
    "@id": "asset-671",
    "properties": {
        "name": "Test Asset"
    },
    "dataAddress": {
        "type": "IonosS3",
        "region": "de",
        "bucketName": "providerBucket",
        "blobName": "device1-data.csv"
    }
}
```

To share a folder:
```
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/")
    },
    "@id": "asset-671",
    "properties": {
        "name": "Test Asset"
    },
    "dataAddress": {
        "type": "IonosS3",
        "region": "de",
        "bucketName": "providerBucket",
        "blobName": "folder/",
        filter.includes: "*.csv"
    }
}
```

| Tag name               | Description                                                                                                                                              | Mandatory                                                                   |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| dataAddress.type       | This extension uses the `IonosS3` designation                                                                                                            | Yes                                                                         |
| dataAddress.region     | S3 Bucket region used to retrieve the S3 API endpoint. [Possible values](https://docs.ionos.com/cloud/storage-and-backup/ionos-object-storage/endpoints) | No.  If not send the configuration `edc.ionos.endpoint.region` will be used |
| dataAddress.bucketName | Name of the S3 Bucket used to store the asset data                                                                                                       | Yes                                                                         |
| dataAddress.blobName   | Path to a file or folder on the source S3 Bucket                                                                                                         | Yes                                                                         |
| filter.includes        | Regular expression to filter the files or folders to be copied from the blobName                                                                         | No                                                                          | 
| filter.excludes        | Regular expression to filter the files or folders to be NOT be copied from the blobName                                                                  | No                                                                          |

### DataDestination
To start a transfer using an IONOS S3 Bucket as its data destination, use the following formats:

To transfer to the root folder:
```
{
    "@context":{
        "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "connectorId":"provider",
    "counterPartyAddress":"http://localhost:8282/protocol",
    "contractId":"3186afb5-7b10-4665-b07b-233f5665eb98",
    "protocol":"dataspace-protocol-http",
    "transferType": "IonosS3-PUSH",
    "dataDestination":{
        "type":"IonosS3",
        "keyName":"4fc5ecaf-6630-4ce5-aacb-f42778a6a65b",
        "region": "de",
        "bucketName":"consumerBucket"
    }
}
```

To transfer to a folder:
```
{
    "@context":{
        "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "connectorId":"provider",
    "counterPartyAddress":"http://localhost:8282/protocol",
    "contractId":"3186afb5-7b10-4665-b07b-233f5665eb98",
    "protocol":"dataspace-protocol-http",
    "transferType": "IonosS3-PUSH",
    "dataDestination":{
        "type":"IonosS3",
        "keyName":"4fc5ecaf-6630-4ce5-aacb-f42778a6a65b",
        "region": "de",
        "bucketName":"consumerBucket",
        "path": "subFolder/"
    }
}
```

Note: Only the PUSH transfer type is supported.

| Tag name               | Description                                                                                                                                              | Mandatory                                                                   |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| transferType           | This extension uses the `IonosS3-PUSH` designation                                                                                                       | Yes                                                                         |
| dataAddress.type       | This extension uses the `IonosS3` designation                                                                                                            | Yes                                                                         |
| dataAddress.keyName    | Key name used to store the temporary S3 Keys during the transfer. Need to be an unique value.                                                            | Yes                                                                         |
| dataAddress.region     | S3 Bucket region used to retrieve the S3 API endpoint. [Possible values](https://docs.ionos.com/cloud/storage-and-backup/ionos-object-storage/endpoints) | No. If not send, the configuration `edc.ionos.endpoint.region` will be used |
| dataAddress.bucketName | Name of the destination S3 Bucket, to receive the transferred data                                                                                       | Yes                                                                         |
| dataAddress.path       | Path of a folder, on the destination S3 Bucket, to receive the transferred data                                                                          | No                                                                          | 

Note: the scope of this repo is NOT to explain the complete flows (and payloads) of the EDC Connector. If you want to know more please take a look at the [Eclipse EDC Samples](https://github.com/eclipse-edc/Samples).