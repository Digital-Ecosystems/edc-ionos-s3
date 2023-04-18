#!/bin/bash

set -e

NAMESPACE="edc-ionos-s3"

if [[ -n "$TF_VAR_namespace" ]]; then
   NAMESPACE=$TF_VAR_namespace
fi

while [[ $(kubectl --kubeconfig=$TF_VAR_kubeconfig -n $NAMESPACE get pods -l 'app.kubernetes.io/name'=vault -o 'jsonpath={...status.phase}') != "Running" ]]; do
   sleep 10
done

# Check if Vault is already initialized
INITIALIZED=$(kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault status -format=json | jq -r ".initialized")

# initialize Vault if not already initialized
if [[ "$INITIALIZED" == "false" ]]; then
   # Initialize Vault
   kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault operator init -key-shares=1 -key-threshold=1 -format=json > vault-keys.json
fi

# Unseal Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault operator unseal $(jq -r ".unseal_keys_b64[]" vault-keys.json)

# Login to Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault login $(jq -r ".root_token" vault-keys.json)

# Enable KV secrets engine
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault secrets enable -version=2 -path=secret kv

# Add secrets to Vault
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault kv put secret/edc.ionos.access.key content=$TF_VAR_s3_access_key
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault kv put secret/edc.ionos.secret.key content=$TF_VAR_s3_secret_key
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault kv put secret/edc.ionos.endpoint content=$TF_VAR_s3_endpoint
kubectl --kubeconfig=$TF_VAR_kubeconfig exec --namespace $NAMESPACE -it vault-0 -- vault kv put secret/edc.ionos.token content=$TF_VAR_s3_token
