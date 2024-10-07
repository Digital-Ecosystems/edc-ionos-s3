# IONOS S3 Extension for Eclipse Dataspace Connector

This repository contains the IONOS S3 Extension that works with the Eclipse Dataspace Connector allowing operations into the IONOS S3 Storage.

Disclaimer: The code of this repo is provided on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing this code and assume any risks associated with Your exercise of permissions. For more information check the License.

Please refer to the official [site](https://github.com/ionos-cloud/edc-ionos-s3).

## Based on the following

- [https://github.com/eclipse-dataspaceconnector/DataSpaceConnector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) - v0.4.1;
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
It is required to configure an `Access key` and a `Secret Access Key` from the IONOS S3 storage service.

The credentials can be found/configured in one of the following:
- Vault;
- Properties file;
- Java arguments;
- Environment Variables (`IONOS_ACCESS_KEY`, `IONOS_SECRET_KEY` and `IONOS_TOKEN`);

It is required to configure those parameters:

| Parameter name                      | Description                                                                                                                                   | Mandatory                                            |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| `edc.ionos.access.key`              | IONOS Access Key Id to access S3                                                                                                              | Yes if the context is accessing file                 |
| `edc.ionos.secret.access.key`       | IONOS Secret Access Key to access S3                                                                                                          | Yes if the context is accessing file                 |
| `edc.ionos.token`                   | IONOS token to allow S3 provisioning                                                                                                          | Yes if the context is provisioning access for others |
| `edc.ionos.endpoint.region`         | IONOS S3 endpoint region. Refer to [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/endpoints) for further information. | No, the default value is "de"                        |
| `edc.ionos.max.files`               | Maximum number of files retrieved by list files function.                                                                                     | No, the default value is 5,000 files                 |
| `edc.ionos.key.validation.attempts` | Maximum number of attemps to validate a temporary key after its creation.                                                                     | No, the default values is 10 attempts                |
| `edc.ionos.key.validation.delay`    | Time to wait (in milisseconds) before each key validation attempt. In each new attempt the delay is multiplied by the attempt number.         | No, the default value is 3,000 (3 seconds)           |

To create the token please take a look at the following [documentation](./ionos_token.md).

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

## Examples
In order to see working examples go to [edc-ionos-samples](https://github.com/Digital-Ecosystems/edc-ionos-samples).

## Deploying to IONOS Kubernetes
Check the [deployment readme](https://github.com/ionos-cloud/edc-ionos-s3/tree/main/deployment/README.md) to see how to deploy the Connector locally or to an external Kubernetes cluster.

