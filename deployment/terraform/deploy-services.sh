#!/bin/bash

set -e

# Check for environment variables
if [ -z `printenv TF_VAR_kubeconfig` ]; then
    echo "Stopping because TF_VAR_kubeconfig is undefined"
    exit 1
fi

if [ -z `printenv TF_VAR_s3_access_key` ]; then
    echo "Stopping because TF_VAR_s3_access_key is undefined"
    exit 1
fi
if [ -z `printenv TF_VAR_s3_secret_key` ]; then
    echo "Stopping because TF_VAR_s3_secret_key is undefined"
    exit 1
fi
if [ -z `printenv TF_VAR_s3_endpoint` ]; then
    echo "Stopping because TF_VAR_s3_endpoint is undefined"
    exit 1
fi
if [ -z `printenv TF_VAR_ionos_token` ]; then
    echo "Stopping because TF_VAR_ionos_token is undefined"
    exit 1
fi

# Deploy vault
cd vault-deploy
terraform init
terraform apply -auto-approve

# Initialize vault
cd ../vault-init
terraform init
terraform apply -auto-approve

# Deploy ionos s3
cd ../ionos-s3-deploy
terraform init
terraform apply -auto-approve

# Configure public address
cd ../configure-public-address
terraform init
terraform apply -auto-approve
