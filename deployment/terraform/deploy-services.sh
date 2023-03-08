#!/bin/bash

# This script is used to build the cloud landscape for the federated catalogue.
terraform init && terraform refresh && terraform plan && terraform apply -auto-approve
