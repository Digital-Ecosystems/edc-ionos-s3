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

```sh
# Required configuration
export TF_VAR_kubeconfig='path to kubeconfig'

export TF_VAR_image_repository=''
export TF_VAR_image_tag=''

export TF_VAR_s3_access_key=''
export TF_VAR_s3_secret_key=''
export TF_VAR_s3_endpoint='' # e.g. s3-eu-central-1.ionoscloud.com
export TF_VAR_ionos_token='' # curl -s -u 'USERNAME:PASSWORD' https://api.ionos.com/auth/v1/tokens/generate | jq -r '.token'
```

***

## Deploy

All commands paths are relative to the current directory where this readme is located.

### 1. Create ImagePullSecret
Before executing the command replace ```<path to docker config json file>``` with real path.

```sh
kubectl create namespace edc-ionos-s3
kubectl create secret -n edc-ionos-s3 generic regcred --from-file=.dockerconfigjson=<path to docker config json file> --type=kubernetes.io/dockerconfigjson
```

### 2. Install the EDC Ionos S3 services

To install the services run the script ```deploy-services.sh``` in ```terraform``` directory.

```sh
cd terraform
./deploy-services.sh
```

### 3. Vault keys
After the services are installed you will have ```vault-keys.json``` file containing the vault keys in ```terraform``` directory.
