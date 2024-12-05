#!/bin/bash

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

# Destroy ionos postgresql cluster
cd ../ionos-postgresqlaas
terraform init
terraform destroy -auto-approve

# Destroy postgresql
cd ../postgresql-deploy
terraform init
terraform destroy -auto-approve

# Destroy db-scripts
cd ../db-scripts
terraform init
terraform destroy -auto-approve

cd ../

# remove terraform state
rm -rf ./configure-public-address/.terraform
rm -f ./configure-public-address/terraform.tfstate
rm -f ./configure-public-address/.terraform.lock.hcl
rm -f ./configure-public-address/terraform.tfstate.backup

rm -rf ./ionos-s3-deploy/.terraform
rm -f ./ionos-s3-deploy/terraform.tfstate
rm -f ./ionos-s3-deploy/.terraform.lock.hcl
rm -f ./ionos-s3-deploy/terraform.tfstate.backup

rm -rf ./vault-init/.terraform
rm -f ./vault-init/terraform.tfstate
rm -f ./vault-init/.terraform.lock.hcl
rm -f ./vault-init/terraform.tfstate.backup

rm -rf ./vault-deploy/.terraform
rm -f ./vault-deploy/terraform.tfstate
rm -f ./vault-deploy/.terraform.lock.hcl
rm -f ./vault-deploy/terraform.tfstate.backup

rm -rf ./ionos-postgresqlaas/.terraform
rm -f ./ionos-postgresqlaas/terraform.tfstate
rm -f ./ionos-postgresqlaas/.terraform.lock.hcl
rm -f ./ionos-postgresqlaas/terraform.tfstate.backup

rm -rf ./postgresql-deploy/.terraform
rm -f ./postgresql-deploy/terraform.tfstate
rm -f ./postgresql-deploy/.terraform.lock.hcl
rm -f ./postgresql-deploy/terraform.tfstate.backup

rm -rf ./db-scripts/.terraform
rm -f ./db-scripts/terraform.tfstate
rm -f ./db-scripts/.terraform.lock.hcl
rm -f ./db-scripts/terraform.tfstate.backup

rm -f vault-init/vault-keys.json
rm -f vault-init/vault-tokens.json
kubectl --kubeconfig $TF_VAR_kubeconfig delete namespace $TF_VAR_namespace

echo "Undeployment complete"