#!/bin/bash

set -e

echo "Creating database $TF_VAR_pg_database"
set +e
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-database --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="postgres" --command="CREATE DATABASE $TF_VAR_pg_database;"
set -e

echo "Creating accesstokendata-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-accesstokendata --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./accesstokendata-store/schema.sql

echo "Creating asset-index tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-asset-index --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./asset-index/schema.sql

echo "Creating contract-definition-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-contract-definition --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./contract-definition-store/schema.sql

echo "Creating contract-negotiation-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-contract-negotiation --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./contract-negotiation-store/schema.sql

echo "Creating data-plane-instance-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-data-plane-instance --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./data-plane-instance-store/schema.sql

echo "Creating data-plane-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-data-plane --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./data-plane-store/schema.sql

echo "Creating edr-index tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-edr --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./edr-index/schema.sql

echo "Creating policy-definition-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-policy-definition --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./policy-definition-store/schema.sql

echo "Creating transfer-process-store tables"
kubectl --kubeconfig=$TF_VAR_kubeconfig run -n $TF_VAR_namespace --timeout=120s -i postgres-create-transfer-process --rm --image=postgres:latest --env="PGUSER=$TF_VAR_pg_username" --env="PGPASSWORD=$TF_VAR_pg_password" --env="PGHOST=$TF_VAR_pg_host" -- psql --dbname="$TF_VAR_pg_database" < ./transfer-process-store/schema.sql
