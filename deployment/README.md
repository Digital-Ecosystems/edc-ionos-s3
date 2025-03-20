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
- [jq](https://jqlang.org/) 
- [Kubernetes cluster](https://kubernetes.io/docs/setup/) - **Note:** follow instructions of this [link
](https://github.com/Digital-Ecosystems/ionos-kubernetes-cluster) to deploy a IONOS kubernetes cluster
- S3 account

***

These are the services that are deployed:
- [Vault](https://www.vaultproject.io/)
- [EDC-Ionos-S3](https://github.com/Digital-Ecosystems/edc-ionos-s3)
- [PostgreSQL](https://www.postgresql.org/)

***

## Configuration

Set environment variables

**Some notes:**  
- You will need docker image of the EDC Ionos S3 connector pushed to a repository. If you don't have one, you can build it following the instructions in the [readme](/connector/README.md);
- To create the IONOS token please take a look at the following [documentation](/ionos_token.md);
- If you are deploying multiple EDC Connectors on the same Kubernetes cluster, make sure **TF_VAR_namespace** and **TF_VAR_vault_name** parameters are unique for each Connector.
- The *TF_VAR_ionos_token*, *TF_VAR_s3_access_key* and *TF_VAR_s3_secret_key* parameters must be connected to the same IONOS DCD user and account.
- The IONOS_S3 connector automatically creates S3 access keys for each file transfer. The limit in DCD is usually 5, so make sure there are less than 5 keys before initiating a file transfer.
- **WARNING**: For **TF_VAR_persistence_type** if you choose **None** the data will be lost if the container pods are restarted.

```sh
# Required configuration
export TF_VAR_namespace='edc-ionos-s3'
export TF_VAR_kubeconfig='path to kubeconfig'
export TF_VAR_persistence_type='PostgreSQLaaS' # 'PostgreSQLaaS', 'PostgreSQL' or 'None'
export TF_VAR_vault_name='vault'  # optional if only 1 connector per cluster
export TF_VAR_vault_token_ttl='30m' # vault token time to live
export TF_VAR_s3_access_key='' # S3 access key
export TF_VAR_s3_secret_key='' # S3 secret key
export TF_VAR_s3_endpoint_region='' # s3 endpoint region (e.g. de)
export TF_VAR_ionos_token='' # IONOS Cloud token, for further information: https://docs.ionos.com/cloud/managed-services/s3-object-storage/endpoints

# Required only if persistence_type is PostgreSQLaaS
export TF_VAR_datacenter_name="Digital Ecosystems"
export TF_VAR_datacenter_location="de/txl"
export TF_VAR_kubernetes_cluster_name="dataspace"
export TF_VAR_kubernetes_node_pool_name="pool2"
export TF_VAR_private_lan_name="k8s-lan"
export TF_VAR_pg_database="edcionos"
export TF_VAR_pg_instances=1
export TF_VAR_pg_cluster_cores=2
export TF_VAR_pg_cluster_ram=2048
export TF_VAR_pg_storage_size=2048
export TF_VAR_pg_storage_type="HDD"
export TF_VAR_pg_version=15
export TF_VAR_pg_display_name="EDC Ionos Postgres"
export TF_VAR_pg_username="edc-ionos"
export TF_VAR_pg_password="edc-ionos-pass"
export TF_VAR_image_repository="ghcr.io/digital-ecosystems/connector"
export TF_VAR_image_tag="v3.1.2-persistence"

# Required if persistence_type is Postgres
export TF_VAR_pg_username="edc-ionos"
export TF_VAR_pg_database="edcionos"
export TF_VAR_pg_password="edc-ionos-pass"
export TF_VAR_image_repository="ghcr.io/digital-ecosystems/connector"
export TF_VAR_image_tag="v3.1.2-persistence"

# Required if persistence_type is None
export TF_VAR_image_repository="ghcr.io/digital-ecosystems/connector"
export TF_VAR_image_tag="v3.1.2"
```

In case you want to configure this Connector without Hashicorp Vault, you need to also set the parameters below in the helm [values.yaml](deployment/helm/edc-ionos-s3/values.yaml):

```yaml
  ionos:
    region: <YOUR-S3-ENDPOINT-REGION>
    accessKey: <YOUR-KEY>
    secretKey: <YOUR-SECRET-KEY>
    token: <IONOS-TOKEN>
```

They should be the same as the ones set in the environment variables. The **ionos.region** is set to the default S3 endpoint region, but it can be changed to any other location.


If you don't want the Connector to be externally accessible, you need to set the following parameters in the helm [values.yaml](deployment/helm/edc-ionos-s3/values.yaml):

```yaml
  service:
    type: ClusterIP
```

This will allocate a public IP address to the Connector. You can then access it on the ports 8181, 8182, 8281, 8282 and 8283.

***

## Deploy

All commands paths are relative to the current directory where this readme is located.

### 1. Deploy the services

To deploy the services run the script ```deploy-services.sh``` in ```terraform``` directory.

```sh
cd terraform
./deploy-services.sh
```
### 2. Undeploy the services

To undeploy the services run the script ```undeploy-services.sh``` in ```terraform``` directory.

```sh
cd terraform
./undeploy-services.sh
```
