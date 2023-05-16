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
export TF_VAR_s3_namespace='edc-ionos-s3'
export TF_VAR_kubeconfig='path to kubeconfig'

export TF_VAR_s3_access_key='' # S3 access key
export TF_VAR_s3_secret_key='' # S3 secret key
export TF_VAR_s3_endpoint='' # s3 endpoint (e.g. s3-eu-central-1.ionoscloud.com)
export TF_VAR_ionos_token='' # IONOS Cloud token
```

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
