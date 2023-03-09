***
# EDC Ionos S3 deployment

This document describes how to deploy EDC Ionos S3 on IONOS DCD.

***

### Requirements
- [Helm](https://helm.sh/docs/intro/install/)
- [Terraform](https://developer.hashicorp.com/terraform/downloads)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** You can use the terraform script in [General-des-development](https://github.com/Digital-Ecosystems/general-des-development) repository to deploy a kubernetes cluster on IONOS DCD.

***

These are the services that are deployed:
- [Vault](https://www.vaultproject.io/)
- [EDC-Ionos-S3](https://github.com/Digital-Ecosystems/edc-ionos-s3)

***

## Configuration

Set environment variables

```sh
# Required configuration
export TF_VAR_kubeconfig='path to kubeconfig'
export TF_VAR_s3_access_key=''
export TF_VAR_s3_secret_key=''
export TF_VAR_s3_endpoint=''
```

***

## Deploy

### 1. Install the EDC Ionos S3 services

To install the services run the script ```deploy-services.sh``` in ```terraform``` directory.

```sh
cd terraform
./deploy-services.sh
```

### 2. Vault keys
After the services are installed you will have ```vault-keys.json``` file containing the vault keys in ```terraform``` directory.