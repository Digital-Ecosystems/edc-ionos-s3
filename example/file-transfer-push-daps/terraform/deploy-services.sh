#!/bin/bash

COMMAND=$1

if [ -z $COMMAND ]; then
    COMMAND="apply"
fi

set -e

# Check for environment variables
if [ -z `printenv TF_VAR_kubeconfig` ]; then
    echo "Stopping because TF_VAR_kubeconfig is undefined"
    exit 1
fi

if [ -z `printenv TF_VAR_registry_name` ]; then
    # generate a random registry name
    export TF_VAR_registry_name=edc-example-8qm2x4dx # edc-example-$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 8 | head -n 1)
    export TF_VAR_container_registry_url=$TF_VAR_registry_name.cr.de-fra.ionos.com
fi

# Build the project
cd build-project
terraform init
terraform $COMMAND -auto-approve
cd ../

# Build and Push the Docker images
cd build-and-push-docker-images
terraform init
terraform $COMMAND -auto-approve
cd ../

# Create DAPS clients
cd create-daps-clients
terraform init
terraform $COMMAND -auto-approve
cd ../

# Deploy vault
cd vault-deploy
terraform init
terraform $COMMAND -auto-approve
cd ../

# Initialize vault
cd vault-init
terraform init
terraform $COMMAND -auto-approve
cd ../

# Deploy ionos s3
cd ionos-s3-deploy
terraform init
terraform $COMMAND -auto-approve
cd ../

# Configure webhook address
cd configure-ionos-s3-webhook-address
terraform init
terraform $COMMAND -auto-approve
cd ../