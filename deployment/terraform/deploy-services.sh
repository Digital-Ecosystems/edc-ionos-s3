#!/bin/bash

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
if [ -z `printenv TF_VAR_s3_token` ]; then
    echo "Stopping because TF_VAR_s3_token is undefined"
    exit 1
fi 

# This script is used to deploy the service with terraform
terraform init && terraform refresh && terraform plan && terraform apply -auto-approve
