#!/bin/bash

echo "Delete kind cluster"
# Delete the kind cluster
kind delete cluster --name edc-ionos-s3

echo "Cleanup hosts file"
# Clean up hosts file
sudo sed -i '/edc-ionos-s3-service/d' /etc/hosts
sudo sed -i '/vault-service/d' /etc/hosts

echo "Cleanup complete"
