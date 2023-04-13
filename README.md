# IONOS S3 Extension for Eclipse Dataspace Connector

This repository contains the IONOS S3 Extension that works with the Eclipse Dataspace Connector allowing operations into the IONOS S3 Storage.



## Based on the following

- [https://github.com/eclipse-dataspaceconnector/DataSpaceConnector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) - v0.0.1-milestone-8;
- [International Data Spaces](https://www.internationaldataspaces.org);
- [GAIA-X](https://gaia-x.eu) project;

## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 11 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;

## Folders Description

### `connector`
Contains the required instructions to create an EDC docker image with the IONOS S3 Extension.

### `edc-ionos-extension`
Contains the source code of the IONOS S3 Extension.

### `example`
Contains an example with a file transfer process between two S3 buckets.

### `gradle/wrapper`
Contains gradle's files required for the building process.

## Dependencies and Configurations
### Dependencies
The extension has the following dependencies:

| Module name                          | Description                                                      |
|-----------------------------------------|------------------------------------------------------------------|
| `edc-ionos-extension:provision-ionos-s3`                    | Provisioning operations for IONOS S3 storage     |
| `edc-ionos-extension:data-plane-ionos-s3`             | Copy data do and from IONOS S3 buckets |
| `org.eclipse.edc:api-observability`             | Health data regarding the state of the connector |
| `org.eclipse.edc:auth-tokenbased`             | Securing the API |
| `org.eclipse.edc:api-control-plane-core`             | Main features of the control plane | 
| `org.eclipse.edc:configuration-filesystem`             | Configuration file features | 
| `org.eclipse.edc:http`             | HTTP support | 
| `org.eclipse.edc:data-management-api`             | EDC asset and contract management |
| `org.eclipse.edc:data-plane-core`             | Main features of the data plane |
| `org.eclipse.edc:data-plane-selector-client`             | Offers several implementations for the data plane selector |
| `org.eclipse.edc:data-plane-selector-core`             | Main features of the data plane selector |
| `org.eclipse.edc:ids`             | Support IDS |
| `de.fraunhofer.iais.eis.ids.infomodel:java`             | IDS Information Model for Java |

### Configurations
It is required to configure an `Access key` and a `Secret Access Key` from the IONOS S3 storage service.

The credentials can be found/configured in one of the following:
- Vault;
- Properties file;
- Java arguments;
- Environment Variables (`IONOS_ACCESS_KEY`, `IONOS_SECRET_KEY` and `IONOS_TOKEN`);

It is required to configure those parameters:

| Parameter name                          | Description                            | Mandatory  |
|-----------------------------------------|----------------------------------------| ---------- |
| `edc.ionos.access.key`                    | IONOS Access Key Id to access S3     | Yes if the context is accessing file |
| `edc.ionos.secret.access.key`             | IONOS Secret Access Key to access S3 | Yes if the context is accessing file |
| `edc.ionos.token`                         | IONOS token to allow S3 provisioning | Yes if the context is provisioning access for others |

To create the token please take a look at the following [documentation](./ionos_token.md).

## Building and Running

```bash
git clone [TBD]
cd EDC-IONOS-Extension
./gradlew clean build
```

```bash
cd connector
java -Dedc.fs.config=resources/config.properties -jar build/libs/dataspace-connector.jar
```

## Example
In order to see a working example, go to the [example](./example/README.md) folder.

## Deploying to IONOS Kubernetes
Check this [document](./k8s.md) to see how to deploy the Connector into a IONOS Kubernetes cluster.

Check the [Deployment Readme](./deployment/README.md) to see how to deploy the Connector locally or to an external Kubernetes cluster.

