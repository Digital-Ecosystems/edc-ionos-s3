#!/bin/bash

# Check for environment variables
if [ -z `printenv TF_VAR_kubeconfig` ]; then
    echo "Stopping because TF_VAR_kubeconfig is undefined"
    exit 1
fi

# This script is used to deploy the service with terraform
terraform init && terraform refresh && terraform plan && terraform apply -auto-approve
