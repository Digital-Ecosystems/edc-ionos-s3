# Helm charts

This directory contains required Helm charts and Helm values for the deployment of the IONOS S3 Extension.

## Manually deploying the IONOS S3 Extension to a Kubernetes cluster

The IONOS S3 Extension can be deployed to a Kubernetes cluster using the Helm charts in this directory.

### Requirements
- [Helm](https://helm.sh/docs/intro/install/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [Kubernetes cluster](https://kubernetes.io/docs/setup/)


### Deploy

1. Install Hashicorp Vault Helm chart

    ```bash
    helm repo add hashicorp https://helm.releases.hashicorp.com
    helm install vault hashicorp/vault --version 0.19.0 --namespace edc-ionos-s3 --create-namespace

    # Initialize Vault
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault operator init -key-shares=1 -key-threshold=1 -format=json > vault-keys.json

    # Unseal Vault
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault operator unseal $(jq -r ".unseal_keys_b64[]" vault-keys.json)

    # Login to Vault
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault login $(jq -r ".root_token" vault-keys.json)

    # Enable KV secrets engine
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault secrets enable -version=2 -path=secret kv

    # Add secrets to Vault
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.access.key content=
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.secret.key content=
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.endpoint content=
    kubectl exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.token content=
    ```

1. Install IONOS S3 Extension Helm chart

    ```bash
    helm install --namespace edc-ionos-s3 edc-ionos-s3 edc-ionos-s3/
    ```