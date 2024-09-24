#!/bin/bash

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

rm -f vault-init/vault-keys.json
rm -f vault-init/vault-tokens.json
