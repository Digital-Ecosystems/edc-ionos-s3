#!/bin/bash

# load config
source .G02-config

# healthcheck
curl -s http://$PROVIDER_IP:8181/api/check/health|jq '.'
curl -s http://$CONSUMER_IP:8181/api/check/health|jq '.'

# create the asset
ASSET_ID=$(pwgen -N1 12)
# ASSET_ID=assetId

echo "Creating asset ID='$ASSET_ID' ..."
curl -vv --header 'X-API-Key: password' \
-d '{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "edc:asset": {
    "@id": "'$ASSET_ID'",
    "properties": {
      "edc:name": "product name",
      "edc:contenttype": "application/json",
      "edc:description": "product description",
      "edc:version": "v1.2.3"
    }
  },
  "edc:dataAddress": {
    "edc:bucketName": "'$PROVIDER_BUCKET'",
    "edc:container": "'$PROVIDER_BUCKET'",
    "edc:blobName": "'$FILENAME'",
    "edc:storage": "s3-eu-central-1.ionoscloud.com",
    "edc:name": "'$FILENAME'",
    "edc:type": "IonosS3",
    "edc:keyName": "'$FILENAME'"
  }
}' -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/assets -s | jq

# create the policy
POLICY_ID=$(pwgen -N1 12)
POLICY_UUID=$(/usr/bin/uuidgen)
echo 'Creating policy ID="'$POLICY_ID'" ...'
curl -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@id": "'$POLICY_ID'",
    "edc:policy": {
        "@context": "http://www.w3.org/ns/odrl.jsonld",
        "@type": "odrl:Set",
        "odrl:permission": [
            {
                "odrl:action": {
                    "odrl:type": "USE"
                },
                "odrl:edctype": "dataspaceconnector:permission",
                "odrl:target": "'$ASSET_ID'"
            }
        ]
    }
}' -H 'X-API-Key: password' \
  -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/policydefinitions

# create the contractoffer
echo ""
echo "Creating contractoffer ..."
CONTRACTOFFER_ID=$(pwgen -N1 12)
curl -d '{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@id": "'$CONTRACTOFFER_ID'",
    "@type": "edc:ContractDefinition",
    "edc:accessPolicyId": "'$POLICY_ID'",
    "edc:assetsSelector": [],
    "edc:contractPolicyId": "'$POLICY_ID'"
}' -H 'X-API-Key: password' \
 -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/contractdefinitions

# fetch the catalog
echo ""
echo "Fetching the catalog ..."
curl -X POST "http://$CONSUMER_IP:8182/management/v2/catalog/request" \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
-d '{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "providerUrl": "http://'$PROVIDER_IP':8281/protocol",
  "protocol": "dataspace-protocol-http"
}' -s | jq '. | select(.key == "@id")'
#| jq '. | select(.key == ["@id"]'
exit 0
# OFFER=$(curl -X POST "http://$CONSUMER_IP:8182/management/v2/catalog/request" \
# --header 'X-API-Key: password' \
# --header 'Content-Type: application/json' \
# -d '{
#   "@context": {
#     "edc": "https://w3id.org/edc/v0.0.1/ns/"
#   },
#   "providerUrl": "http://'$PROVIDER_IP':8281/protocol",
#   "protocol": "dataspace-protocol-http"
# }' -s |jq '."dcat:dataset" |.[] | .[] | select(.["@id"] == "1dd1a65f-3689-456c-b948-29b9647ff919")')

# OFFER=$(echo "$OFFER" | sed 's/^=//')
echo "OFFER=$OFFER"
exit 0

OFFER_HASPOLICY=$(echo "$OFFER" | jq '."odrl:hasPolicy"')
OFFER_HASPOLICY_ID=$(echo "$OFFER" |jq '."odrl:hasPolicy" |."@id"')
OFFER_EDC_ID=$(echo "$OFFER" |jq '."edc:id"')
# echo "OFFER_HASPOLICY=$OFFER_HASPOLICY"
# echo "OFFER_HASPOLICY_ID=$OFFER_HASPOLICY_ID"
# echo "OFFER_EDC_ID=$OFFER_EDC_ID"
# exit 0

# contract negotiation
OFFER_ID="$CONTRACTOFFER_ID:$(/usr/bin/uuidgen)"
echo ""
echo "Negotiating the contract with OFFER_ID=$OFFER_HASPOLICY_ID ..."
JSON_PAYLOAD=$(cat <<-EOF
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "edc:NegotiationInitiateRequestDto",
    "edc:connectorAddress": "http://$PROVIDER_IP:8281/protocol",
    "edc:connectorId": "provider",
    "edc:consumerId": "consumer",
    "edc:offer": {
        "@type": "edc:ContractOfferDescription",
        "edc:assetId": "$ASSET_ID",
        "edc:offerId": $OFFER_HASPOLICY_ID,
        "edc:policy": $OFFER_HASPOLICY},
    "edc:protocol": "dataspace-protocol-http",
    "edc:providerId": "provider"
}
EOF
)
echo "Generated contract offer: $JSON_PAYLOAD"
echo "Creating contract offer..."
# curl --header 'X-API-Key: password' -X POST -H 'content-type: application/json' -d "$JSON_PAYLOAD" "http://$CONSUMER_IP:8182/management/v2/contractnegotiations"
# exit 0

ID=$(curl -s --header 'X-API-Key: password' -X POST -H 'content-type: application/json' -d "$JSON_PAYLOAD" "http://$CONSUMER_IP:8182/management/v2/contractnegotiations" | jq -r '.["@id"]')
echo "Contract negitiation ID=$ID. JSON_PAYLOAD=$JSON_PAYLOAD"

# get contract agreement ID
sleep 5
curl -X GET "http://$CONSUMER_IP:8182/management/v2/contractnegotiations/$ID" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json'
exit 0

CONTRACT_AGREEMENT_ID=$(curl -X GET "http://$CONSUMER_IP:8182/management/v2/contractnegotiations/$OFFER_HASPOLICY_ID" \
	--header 'X-API-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.["@id"]')
echo ""
echo "Contract agreement ID: $CONTRACT_AGREEMENT_ID"

# file transfer
curl -i -X POST "http://$CONSUMER_IP:8182/management/v2/transferprocesses" \
  --header "Content-Type: application/json" \
  --header 'X-API-Key: password' \
  -d @- <<-EOF
  {
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "edc:TransferRequestDto",
    "edc:assetId": "$ASSET_ID",
    "edc:connectorAddress": "http://$PROVIDER_IP:8281/protocol",
    "edc:connectorId": "provider",
    "edc:contractId": "$CONTRACT_AGREEMENT_ID",
    "edc:dataDestination": {
        "@type": "edc:DataAddress",
        "edc:blobName": "$FILENAME",
        "edc:bucketName": "$CONSUMER_BUCKET",
        "edc:container": "$CONSUMER_BUCKET",
        "edc:keyName": "$FILENAME",
        "edc:name": "$FILENAME",
        "edc:storage": "s3-eu-central-1.ionoscloud.com",
        "edc:type": "IonosS3"
    },
    "edc:managedResources": false,
    "edc:protocol": "dataspace-protocol-http"
}
EOF
