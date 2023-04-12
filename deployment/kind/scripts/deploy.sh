#!/bin/bash

# Check if all requirements are installed
if ! command -v helm &> /dev/null
then
    echo "helm could not be found"
    exit
fi

if ! command -v kind &> /dev/null
then
    echo "kind could not be found"
    exit
fi

if ! command -v kubectl &> /dev/null
then
    echo "kubectl could not be found"
    exit
fi

if ! command -v docker &> /dev/null
then
    echo "docker could not be found"
    exit
fi

# Check for environment variables
if [ -z `printenv S3_ACCESS_KEY` ]; then
    echo "Stopping because S3_ACCESS_KEY is undefined"
    exit 1
fi 
if [ -z `printenv S3_SECRET_KEY` ]; then
    echo "Stopping because S3_SECRET_KEY is undefined"
    exit 1
fi 
if [ -z `printenv S3_ENDPOINT` ]; then
    echo "Stopping because S3_ENDPOINT is undefined"
    exit 1
fi

# clean old installation
scripts/cleanup.sh

# Create a kind cluster
kind create cluster --name edc-ionos-s3
kubectl apply -f ./metalLB/metalLB-native.yaml
kubectl wait --for=condition=available --timeout=600s deployment -n metallb-system controller
kubectl apply -f ./metalLB/metalLB.yaml
kubectl create namespace edc-ionos-s3

# Build docker image
cd ../../
./gradlew clean build
docker build -t ghcr.io/edc-ionos-s3/connector:1.0.0 ./connector/
cd ./deployment/kind/
kind load docker-image ghcr.io/edc-ionos-s3/connector:1.0.0 --name edc-ionos-s3

# Deploy Vault
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install -n edc-ionos-s3 --wait vault hashicorp/vault \
    -f ./scripts/vault-values.yaml \
    --version 0.19.0 \
    --create-namespace \
    --kubeconfig=$KUBECONFIG

# Init Vault
export TF_VAR_kubeconfig=$KUBECONFIG
export TF_VAR_s3_access_key=$S3_ACCESS_KEY
export TF_VAR_s3_secret_key=$S3_SECRET_KEY
export TF_VAR_s3_endpoint=$S3_ENDPOINT
export TF_VAR_s3_token=$S3_TOKEN
../terraform/vault-init/vault-init.sh

# Deploy IONOS-S3
helm install -n edc-ionos-s3 --wait edc-ionos-s3 ../helm/edc-ionos-s3 \
    -f ./scripts/edc-s3-values.yaml \
    --create-namespace \
    --set edc.vault.hashicorp.token=$(jq -r .root_token ./vault-keys.json) \
    --kubeconfig=$KUBECONFIG

echo "$(kubectl get svc -n edc-ionos-s3 edc-ionos-s3 -o jsonpath='{.status.loadBalancer.ingress[0].ip}') edc-ionos-s3-service" | sudo tee -a /etc/hosts
echo "$(kubectl get svc -n edc-ionos-s3 vault-ui -o jsonpath='{.status.loadBalancer.ingress[0].ip}') vault-service" | sudo tee -a /etc/hosts

echo "----------------------------------"

# EDC Ionos S3 service address
echo "API URL: http://edc-ionos-s3-service:8181"
echo "Management URL: http://edc-ionos-s3-service:8182"
echo "IDS URL: http://edc-ionos-s3-service:8282"

echo "----------------------------------"

# Vault service address
echo "Vault URL: http://vault-service:8200"
