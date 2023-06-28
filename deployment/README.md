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
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** follow instructions of this [link
](https://github.com/Digital-Ecosystems/ionos-kubernetes-cluster) to deploy a IONOS kubernetes cluster
- S3 account

***

These are the services that are deployed:
- [Vault](https://www.vaultproject.io/)
- [EDC-Ionos-S3](https://github.com/Digital-Ecosystems/edc-ionos-s3)

***

## Configuration

Set environment variables

**Some notes:**  
- You will need docker image of the EDC Ionos S3 connector pushed to a repository. If you don't have one, you can build it following the instructions in the [readme](/connector/README.md);
- To create the IONOS token please take a look at the following [documentation](/ionos_token.md);
- If you are deploying multiple EDC Connectors on the same Kubernetes cluster, make sure **TF_VAR_namespace** and **TF_VAR_vaultname** parameters are unique for each Connector.
- The *TF_VAR_ionos_token*, *TF_VAR_s3_access_key* and *TF_VAR_s3_secret_key* parameters must be connected to the same IONOS DCD user and account.
- The IONOS_S3 connector automatically creates S3 access keys for each file transfer. The limit in DCD is usually 5, some make sure there are less than 5 keys in before initiating a file transfer.
- 

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
