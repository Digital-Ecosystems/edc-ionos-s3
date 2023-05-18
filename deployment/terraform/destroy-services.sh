#!/bin/bash

set -e

# Check for environment variables
if [ -z `printenv TF_VAR_kubeconfig` ]; then
    echo "Stopping because TF_VAR_kubeconfig is undefined"
    exit 1
fi

# Deploy vault
cd vault-deploy
terraform init
terraform destroy -auto-approve

# Initialize vault
cd ../vault-init
terraform init
terraform destroy -auto-approve

# Deploy ionos s3
cd ../ionos-s3-deploy
terraform init
terraform destroy -auto-approve

kubectl --kubeconfig $TF_VAR_kubeconfig delete namespace $TF_VAR_namespace
rm vault-init/vault-keys.json

