#!/bin/bash

set -e

echo "Checking for required environment variables"
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

if [ -z `printenv TF_VAR_persistence_type` ]; then
    echo "Stopping because TF_VAR_persistence_type is undefined"
    exit 1
fi

# Check for postgres environment variables
if [ "$TF_VAR_persistence_type" == "PostgreSQLaaS" ]; then
    if [ -z `printenv TF_VAR_datacenter_name` ]; then
        echo "Stopping because TF_VAR_datacenter_name is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_datacenter_location` ]; then
        echo "Stopping because TF_VAR_datacenter_location is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_kubernetes_node_pool_name` ]; then
        echo "Stopping because TF_VAR_kubernetes_node_pool_name is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_kubernetes_cluster_name` ]; then
        echo "Stopping because TF_VAR_kubernetes_cluster_name is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_private_lan_name` ]; then
        echo "Stopping because TF_VAR_private_lan_name is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_instances` ]; then
        echo "Stopping because TF_VAR_pg_instances is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_cluster_cores` ]; then
        echo "Stopping because TF_VAR_pg_cluster_cores is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_cluster_ram` ]; then
        echo "Stopping because TF_VAR_pg_cluster_ram is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_storage_size` ]; then
        echo "Stopping because TF_VAR_pg_storage_size is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_version` ]; then
        echo "Stopping because TF_VAR_pg_version is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_display_name` ]; then
        echo "Stopping because TF_VAR_pg_display_name is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_username` ]; then
        echo "Stopping because TF_VAR_pg_username is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_password` ]; then
        echo "Stopping because TF_VAR_pg_password is undefined"
        exit 1
    fi

    if [ -z `printenv TF_VAR_pg_storage_type` ]; then
        echo "Stopping because TF_VAR_pg_storage_type is undefined"
        exit 1
    fi
fi

echo "Deploying vault"
# Deploy vault
cd vault-deploy
terraform init
terraform apply -auto-approve

echo "Initializing vault"
# Initialize vault
cd ../vault-init
terraform init
terraform apply -auto-approve

if [ "$TF_VAR_persistence_type" == "PostgreSQLaaS" ]; then
    echo "Deploying ionos postgresqlaas"
    # Create Ionos Postgres cluster
    cd ../ionos-postgresqlaas
    terraform init
    terraform apply -auto-approve

    export TF_VAR_pg_host=$(terraform output -raw postgres_host)
    kubectl run -i postgres-create-database --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="postgres" --command="CREATE DATABASE edcionos;"

    kubectl run -i postgres-restore-database --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="edcionos" < ../db-scripts/init.sql
fi

if [ "$TF_VAR_persistence_type" == "Postgres" ]; then
    echo "Deploying postgres"
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm install postgres bitnami/postgresql --set postgresql.auth.username=$TF_VAR_pg_username --set postgresql.auth.password=$TF_VAR_pg_password --set postgresql.auth.database=$TF_VAR_pg_database

    kubectl run -i postgres-restore-database --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="edcionos" < ../db-scripts/init.sql

    export TF_VAR_pg_host=$(terraform output -raw postgres_host)
fi

echo "Deploying ionos s3"
# Deploy ionos s3
cd ../ionos-s3-deploy
terraform init
terraform apply -auto-approve

echo "Configuring public address"
# Configure public address
cd ../configure-public-address
terraform init
terraform apply -auto-approve

echo "Deployment complete"