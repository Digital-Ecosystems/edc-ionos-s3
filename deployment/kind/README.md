# Kind 

This directory contains Kubernetes YAML manifests for the deployment a single instance of EDC Ionos S3 on a local Kind cluster.

### Requirements
- [Kind](https://kind.sigs.k8s.io/)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/get-docker/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- S3 account
- SUDO rights on the local system
***

These are the services that are deployed:
- [Vault](https://www.vaultproject.io/)
- [EDC-Ionos-S3](https://github.com/Digital-Ecosystems/edc-ionos-s3)

***

## Configuration

Set environment variables

```sh
# Required configuration
export KUBECONFIG=path/to/kubeconfig
export S3_ACCESS_KEY=''
export S3_SECRET_KEY=''
export S3_ENDPOINT=''
export S3_TOKEN=''
```

***

## Deploy

All commands paths are relative to the current directory where this readme is located.

### 1. Install the EDC Ionos S3 services

To install the services run the script ```deploy.sh``` in ```scripts``` directory.

```sh
./scripts/deploy.sh
```

### 2. Vault keys
After the services are installed you will have ```vault-keys.json``` file containing the vault keys in same directory.

***

## Delete

To delete the local environment run the script ```cleanup.sh``` in ```scripts``` directory.

```sh
./scripts/cleanup.sh
```