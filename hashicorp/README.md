# IONOS S3 Configuration with Hashicorp

This document shows how to configure an Hashicorp vault that will be used by the IONOS S3 Extension of the EDC.

## Requirements

You will need the following:
- IONOS account, for cloud deployment;
- kubectl, for cloud deployment;
- helm, for cloud deployment;
- Hashicorp vault;
- vault CLI;
- Docker, for local deployment;
- GIT;
- Linux shell or PowerShell;

### vault CLI
In order to populate the Hashicorp vault with the required configuration for the EDC IONOS S3 Extension you will need to install the vault CLI. You can do the following;
```console
wget -O- https://apt.releases.hashicorp.com/gpg | gpg --dearmor | sudo tee /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install vault
```

Or, you can use `kubectl`. See bellow.

## Deployment of the Hashicorp vault

### Local deployment

If you want to do this kind of deployment, you can use the `docker-compose.yml` file and run it with docker.

```bash
cd hashicorp
docker-compose up
```
### Cloud deployment

```bash
# helm
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault --version 0.19.0

# Initialize Vault
kubectl exec -it vault-0 -- vault operator init -key-shares=1 -key-threshold=1 -format=json > vault-keys.json

# Unseal Vault
kubectl exec -it vault-0 -- vault operator unseal $(jq -r ".unseal_keys_b64[]" vault-keys.json)

# Login to Vault
kubectl exec -it vault-0 -- vault login $(jq -r ".root_token" vault-keys.json)

# Enable KV secrets engine
kubectl exec -it vault-0 -- vault secrets enable -version=2 -path=secret kv
```
Note:
- you may have to configure the `KUBECONFIG` variable to connect to your Kubernetes cluster;

## Populate the Hashicorp vault
### Using vault CLI
```console
export VAULT_ADDR='<IP_ADDRESS_OF_VAULT:VAULT_PORT>'
vault login token=<YOUR VAULT TOKEN>
vault kv put secret/edc.ionos.access.key content=<IONOS-ACCESS>
vault kv put secret/edc.ionos.secret.key content=<IONOS-SECRET>
vault kv put secret/edc.ionos.token content=<IONOS-TOKEN>
vault kv put secret/edc.ionos.endpoint.region content=<IONOS-S3-ENDPOINT-REGION>
```

Note:
- point to your Hashicorp instance (for local deployment you can use http://0.0.0.0:8200 or use the IP address and port that you used for the cloud deployment;
- the `edc.ionos.token`field is only required when you want to do some provisiong tasks;
- the `Root Token` of the Hashicorp vault instance is displayed when you start the service;

### Using kubectl
```console
kubectl exec -it vault-0 -- vault kv put secret/edc.ionos.access.key content=<IONOS-ACCESS>
kubectl exec -it vault-0 -- vault kv put secret/edc.ionos.secret.key content=<IONOS-SECRET>
kubectl exec -it vault-0 -- vault kv put secret/edc.ionos.endpoint.region content=<IONOS-S3-ENDPOINT-REGION>
kubectl exec -it vault-0 -- vault kv put secret/edc.ionos.token content=<IONOS-TOKEN>
```

Note:
- the `edc.ionos.token` field is only required when you want to do some provisiong tasks;
- the `Root Token` of the Hashicorp vault instance is displayed when you start the service;

## Pull Transfers Support

If you need to perform pull transfers on your connector, you need to perform the following steps:

### Using vault CLI
```bash
export VAULT_ADDR=<VAULT_HTTP_ADDRESS>
vault login token=<VAULT_TOKEN>

vault kv put secret/edc.connector.private.key content=@./certs/private.pem
vault kv put secret/edc.connector.public.key content=@./certs/public.pem
```

### Using kubectl
```bash
kubectl exec -it "vault-0" -- vault kv put secret/edc.connector.private.key content="$(cat ./certs/private.pem)"
kubectl exec -it "vault-0" -- vault kv put secret/edc.connector.public.key content="$(cat ./certs/public.pem)"
```
