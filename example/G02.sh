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
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "asset": {
    "@id": "'$ASSET_ID'",
    "properties": {
      "name": "product description",
      "contenttype": "application/json"
    }
  },
  "dataAddress": {
    "bucketName": "'$PROVIDER_BUCKET'",
    "container": "'$PROVIDER_BUCKET'",
    "blobName": "'$FILENAME'",
    "storage": "s3-eu-central-1.ionoscloud.com",
    "name": "'$FILENAME'",
    "type": "IonosS3"
  }
}' -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/assets \
         -s | jq

# create the policy
POLICY_ID=$(pwgen -N1 12)
POLICY_UUID=$(/usr/bin/uuidgen)
echo 'Creating policy ID="'$POLICY_ID'" ...'
curl -d '{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "id": "'$POLICY_ID'",
  "policy": {
    "@type": "set",
    "odrl:permission": [
      {
        "target": "'$ASSET_ID'",
        "action": {
          "type": "USE"
        },
        "edctype": "dataspaceconnector:permission"
      }
    ],
    "odrl:prohibition": [],
    "odrl:obligation": []
  }
}' -H 'X-API-Key: password' \
  -H 'content-type: application/json' http://$PROVIDER_IP:8182/management/v2/policydefinitions

# create the contractoffer
echo ""
echo "Creating contractoffer ..."
CONTRACTOFFER_ID=$(pwgen -N1 12)
curl -d '{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "id": "'$CONTRACTOFFER_ID'",
  "accessPolicyId": "'$POLICY_ID'",
  "contractPolicyId": "'$POLICY_ID'",
  "criteria": []
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
}'

# contract negotiation
OFFER_ID="$CONTRACTOFFER_ID:$(/usr/bin/uuidgen)"
echo ""
echo "Negotiating the contract with OFFER_ID=$OFFER_ID ..."
JSON_PAYLOAD=$(cat <<-EOF
{
	"@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "NegotiationInitiateRequestDto",
  "connectorId": "provider",
  "connectorAddress": "http://$PROVIDER_IP:8281/protocol",
  "protocol": "dataspace-protocol-http",
  "offer": {
    "offerId": "$OFFER_ID",
    "assetId": "$ASSET_ID",
    "policy": {"@id":"$POLICY_UUID",
			"@type": "odrl:Set",
			"odrl:permission": {
				"odrl:target": "$ASSET_ID",
				"odrl:action": {
					"odrl:type": "USE"
				}
			},
			"odrl:prohibition": [],
			"odrl:obligation": [],
			"odrl:target": "1"}
  }
}
EOF
)
ID=$(curl -s --header 'X-API-Key: password' -X POST -H 'content-type: application/json' -d "$JSON_PAYLOAD" "http://$CONSUMER_IP:8182/management/v2/contractnegotiations" | jq -r '.["@id"]')
echo "Contract negitiation ID=$ID. JSON_PAYLOAD=$JSON_PAYLOAD"

exit 0


# get contract agreement ID
sleep 5
CONTRACT_AGREEMENT_ID=$(curl -X GET "http://$CONSUMER_IP:8182/management/v2/contractnegotiations/$ID" \
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
      "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type": "TransferRequestDto",

    "connectorId": "consumer",
    "connectorAddress": "http://$PROVIDER_IP:8281/protocol",
    "contractId": "$CONTRACT_AGREEMENT_ID",
    "protocol": "dataspace-protocol-http",
    "assetId": "$ASSET_ID",
    "managedResources": "false",
    "transferType": {
      "contentType": "application/octet-stream",
      "isFinite": true
    },
    "dataDestination": {
      "properties": {
        "type": "IonosS3",
        "storage":"s3-eu-central-1.ionoscloud.com",
        "bucketName": "$CONSUMER_BUCKET",
        "keyName" : "device1-data.csv"
      }
    }
  }
EOF