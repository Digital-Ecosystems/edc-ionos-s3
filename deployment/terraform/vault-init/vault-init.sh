#!/bin/bash

while [[ $(kubectl --kubeconfig=$TF_VAR_kubeconfig -n edc-ionos-s3 get pods -l 'app.kubernetes.io/name'=vault -o 'jsonpath={...status.phase}') != "Running" ]]; do
   sleep 10
done

# Initialize Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault operator init -key-shares=1 -key-threshold=1 -format=json > vault-keys.json

# Unseal Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault operator unseal $(jq -r ".unseal_keys_b64[]" vault-keys.json)

# Login to Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault login $(jq -r ".root_token" vault-keys.json)

# Enable KV secrets engine
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault secrets enable -version=2 -path=secret kv

# Add secrets to Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.access.key content=$TF_VAR_s3_access_key
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.secret.key content=$TF_VAR_s3_secret_key
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace edc-ionos-s3 -it vault-0 -- vault kv put secret/edc.ionos.endpoint content=$TF_VAR_s3_endpoint