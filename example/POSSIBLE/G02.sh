#!/bin/bash

# Get config from environment variables
source .G02-config

set -e

request_timeout=10

# Make health check requests to both connectors

PROVIDER_HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://$PROVIDER_IP:8181/api/check/health -m $request_timeout)
CONSUMER_HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://$CONSUMER_IP:8181/api/check/health -m $request_timeout)

if [ $PROVIDER_HEALTH_RESPONSE -ne 200 ]; then
  echo "Provider health check failed."
  echo $PROVIDER_HEALTH_RESPONSE
  exit 1
fi

if [ $CONSUMER_HEALTH_RESPONSE -ne 200 ]; then
  echo "Consumer health check failed." 
  echo $CONSUMER_HEALTH_RESPONSE
  exit 1
fi

# Create the asset

asset_id=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1)
echo "Creating asset ID=\"$asset_id\" ..."

create_asset_response=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$PROVIDER_IP:8182/management/v2/assets \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "edc:asset": {
        "@id": "'"$asset_id"'",
        "@type": "edc:Asset",
        "edc:properties": {
            "edc:name": "Name",
            "edc:description": "Description",
            "edc:contenttype": "application/json",
            "edc:version": "v1.2.3"
        }
    },
    "edc:dataAddress": {
        "@type": "edc:DataAddress",
        "edc:bucketName": "'"$PROVIDER_BUCKET"'",
        "edc:container": "'"$PROVIDER_BUCKET"'",
        "edc:blobName": "'"$FILENAME"'",
        "edc:keyName": "'"$FILENAME"'",
        "edc:storage": "s3-eu-central-1.ionoscloud.com",
        "edc:name": "'"$FILENAME"'",
        "edc:type": "IonosS3"
    }
}' \
  -m $request_timeout)

if [ "$create_asset_response" != "200" ]; then
  echo "Asset creation failed with response code: $create_asset_response."
  echo $create_asset_response
  exit 1
fi

# Create the policy 

policy_id=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1)
echo "Creating policy ID=\"$policy_id\" ..."

create_policy_response=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$PROVIDER_IP:8182/management/v2/policydefinitions \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@id": "'"$policy_id"'",
    "edc:policy": {
        "@context": "http://www.w3.org/ns/odrl.jsonld",
        "@type": "odrl:Set",
        "odrl:permission": [
            {
                "odrl:target": "'"$asset_id"'",
                "odrl:action": {
                    "odrl:type": "USE"
                },
                "odrl:edctype": "dataspaceconnector:permission"
            }
        ]
    }
}' \
  -m $request_timeout)

if [ "$create_policy_response" != "200" ]; then
  echo "Policy creation failed with response code: $create_policy_response."
  echo $create_policy_response
  exit 1
fi

# Create the contractoffer

contractoffer_id=1

create_contractoffer_response=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$PROVIDER_IP:8182/management/v2/contractdefinitions \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@id": "'"$contractoffer_id"'",
    "@type": "edc:ContractDefinition",
    "edc:accessPolicyId": "'"$policy_id"'",
    "edc:contractPolicyId": "'"$policy_id"'",
    "edc:assetsSelector": []
}' \
  -m $request_timeout)

if [ "$create_contractoffer_response" != "200" ] && [ "$create_contractoffer_response" != "409" ]; then
  echo "Contract offer creation failed with response code: $create_contractoffer_response."
  
  echo $create_contractoffer_response
  exit 1
fi

# Fetch the catalog

echo "Fetching the catalog ..."

fetch_catalog_response=$(curl -s -X POST http://$CONSUMER_IP:8182/management/v2/catalog/request \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "providerUrl": "http://'"$PROVIDER_IP"':8281/protocol",
    "protocol": "dataspace-protocol-http"
}' \
  -m $request_timeout)

catalog_contractOffer=$(echo "$fetch_catalog_response" | jq '.["dcat:dataset"][0]')

if [ -z "$catalog_contractOffer" ]; then
  echo "New asset fetch failed."
  exit 1
fi

# Contract negotiation

offer_id=$(echo $catalog_contractOffer | jq -r '.["odrl:hasPolicy"] | .["@id"]')
echo "Negotiating the contract with OFFER_ID=\"$offer_id\" ..."

contract_negotiations_response=$(curl -X POST http://$CONSUMER_IP:8182/management/v2/contractnegotiations \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "edc:NegotiationInitiateRequestDto",
    "edc:connectorId": "provider",
    "edc:connectorAddress": "http://'"$PROVIDER_IP"':8281/protocol",
    "edc:consumerId": "consumer",
    "edc:protocol": "dataspace-protocol-http",
    "edc:providerId": "provider",
    "edc:offer": {
        "@type": "edc:ContractOfferDescription",
        "edc:offerId": "'$offer_id'",
        "edc:assetId": "'$(echo $catalog_contractOffer | jq -r '.["edc:id"]')'",
        "edc:policy":'"$(echo $catalog_contractOffer | jq '.["odrl:hasPolicy"]')"'
    }
}' \
  -m $request_timeout)

if [ $? -ne 0 ]; then
  echo "Contract negotiation failed."
  exit 1
fi

contract_negotiations_id=$(echo $contract_negotiations_response | jq -r '.["@id"]')

# Get contract agreement ID

MAX_RETRIES=10
WAIT_SECONDS=5

for ((i=0; i<$MAX_RETRIES; i++)); do
  echo "Requesting status of negotiation"
  sleep $WAIT_SECONDS
  
  get_contract_agreement_response=$(curl http://$CONSUMER_IP:8182/management/v2/contractnegotiations/$contract_negotiations_id \
    -H 'Content-Type: application/json' \
    -H "X-API-Key: password" \
    -m $request_timeout)

  if [ $? -ne 0 ]; then
    echo "Contract agreement failed."
    exit 1
  fi

  echo $get_contract_agreement_response

  STATE=$(echo $get_contract_agreement_response | jq -r '.["edc:state"]')

  if [ "$STATE" == "FINALIZED" ]; then
    contract_agreement_id=$(echo $get_contract_agreement_response | jq -r '.["edc:contractAgreementId"]')
    echo "Contract agreement ID: $contract_agreement_id is FINALIZED."
    break
  elif [[ "$STATE" == "TERMINATING" || "$STATE" == "TERMINATED" ]]; then
    echo "Contract agreement failed."
    exit 1
  elif [ $i -eq $((MAX_RETRIES-1)) ]; then
    echo "Contract agreement failed." 
    exit 1
  fi
done

# File transfer

file_transfer_response=$(curl -X POST http://$CONSUMER_IP:8182/management/v2/transferprocesses \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: password" \
  -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "edc:TransferRequestDto",
    "edc:connectorId": "provider",
    "edc:connectorAddress": "http://'"$PROVIDER_IP"':8281/protocol",
    "edc:contractId": "'"$contract_agreement_id"'",
    "edc:protocol": "dataspace-protocol-http",
    "edc:assetId": "'"$asset_id"'",
    "edc:managedResources": false,
    "edc:dataDestination": {
        "@type": "edc:DataAddress",
        "edc:blobName": "'"$FILENAME"'",
        "edc:bucketName": "'"$CONSUMER_BUCKET"'",
        "edc:container": "'"$CONSUMER_BUCKET"'",
        "edc:keyName": "'"$FILENAME"'",
        "edc:name": "'"$FILENAME"'",
        "edc:storage": "s3-eu-central-1.ionoscloud.com",
        "edc:type": "IonosS3"
    }
}' \
  -m $request_timeout)

TRANSFER_PROCESS_ID=$(echo $file_transfer_response | jq -r '.["@id"]')

# Get transfer process status

MAX_RETRIES=30
WAIT_SECONDS=5

for ((i=0; i<$MAX_RETRIES; i++)); do
  echo "Requesting status of transfer ..."
  sleep $WAIT_SECONDS

  get_transfer_process_response=$(curl http://$CONSUMER_IP:8182/management/v2/transferprocesses/$TRANSFER_PROCESS_ID \
    -H 'Content-Type: application/json' \
    -H "X-API-Key: password" \
    -m $request_timeout)

  STATE=$(echo $get_transfer_process_response | jq -r '.["edc:state"]')

  echo $get_transfer_process_response

  if [ "$STATE" == "COMPLETED" ]; then
    break
  elif [ "$STATE" == "TERMINATING" ]; then
    echo "Transfer failed."
    exit 1
  elif [ $i -eq $((MAX_RETRIES-1)) ]; then
    echo "Transfer failed."
    exit 1 
  fi
done