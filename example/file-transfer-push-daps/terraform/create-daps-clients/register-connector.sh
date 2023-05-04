#!/bin/sh

set -e

KUBERNETES_NAMESPACE="omejdn-daps"
OMJEDN_INSTANCE_NAME="omejdn-server"

OMEJDN_SERVER_POD_NAME=$(kubectl get pods --namespace $KUBERNETES_NAMESPACE -l "app.kubernetes.io/name=$OMJEDN_INSTANCE_NAME,app.kubernetes.io/instance=$OMJEDN_INSTANCE_NAME" -o jsonpath="{.items[0].metadata.name}") || true

if [ -z "$OMEJDN_SERVER_POD_NAME" ]; then
  echo "Could not find the OMEJDN server pod. Please check if the OMEJDN server is deployed."
  exit 1
fi

# Check for KUBECONFIG
if [ -z "$KUBECONFIG" ]; then
  echo "KUBECONFIG is not set. Please set it to the path of your kubeconfig file."
  exit 1
fi

# Check for OMEJDN_SERVER_POD_NAME
if [ -z "$OMEJDN_SERVER_POD_NAME" ]; then
  echo "OMEJDN_SERVER_POD_NAME is not set. Please set it to the name of the DAPS server pod."
  exit 1
fi

if [ ! $# -ge 2 ] || [ ! $# -le 4 ]; then
    echo "Usage: $0 NAME KEYSTORE_PASSWORD (SECURITY_PROFILE) (CERTFILE)"
    exit 1
fi

CLIENT_NAME=$1

if [ -z "$CLIENT_NAME" ]; then
    echo "Client name must not be empty"
    exit 1
fi

KEYSTORE_PASSWORD=$2

if [ -z "$KEYSTORE_PASSWORD" ]; then
    echo "Keystore password must not be empty"
    exit 1
fi

CLIENT_SECURITY_PROFILE=$3
[ -z "$CLIENT_SECURITY_PROFILE" ] && CLIENT_SECURITY_PROFILE="idsc:BASE_SECURITY_PROFILE"

CLIENT_CERT="./connectors/$CLIENT_NAME/keys/$CLIENT_NAME.cert"
mkdir -p ./connectors/$CLIENT_NAME/keys/
if [ -n "$4" ]; then
    [ ! -f "$4" ] && (echo "Cert not found"; exit 1)
    cert_format="DER"
    openssl x509 -noout -in "$4" 2>/dev/null && cert_format="PEM"
    openssl x509 -inform "$cert_format" -in "$4" -text > "$CLIENT_CERT"
else
    openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout "./connectors/$CLIENT_NAME/keys/${CLIENT_NAME}.key" -out "$CLIENT_CERT"
fi

SKI="$(grep -A1 "Subject Key Identifier"  "$CLIENT_CERT" | tail -n 1 | tr -d ' ')"
AKI="$(grep -A1 "Authority Key Identifier"  "$CLIENT_CERT" | tail -n 1 | tr -d ' ')"
CLIENT_ID="$SKI:$AKI"

CLIENT_CERT_SHA="$(openssl x509 -in "$CLIENT_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

mkdir -p ./connectors/$CLIENT_NAME/config/
cat > ./connectors/$CLIENT_NAME/config/clients.yml <<EOF
- client_id: $CLIENT_ID
  client_name: $CLIENT_NAME
  grant_types: client_credentials
  token_endpoint_auth_method: private_key_jwt
  scope: idsc:IDS_CONNECTOR_ATTRIBUTES_ALL
  attributes:
  - key: idsc
    value: IDS_CONNECTOR_ATTRIBUTES_ALL
  - key: securityProfile
    value: $CLIENT_SECURITY_PROFILE
  - key: referringConnector
    value: http://${CLIENT_NAME}.demo
  - key: "@type"
    value: ids:DatPayload
  - key: "@context"
    value: https://w3id.org/idsa/contexts/context.jsonld
  - key: transportCertsSha256
    value: $CLIENT_CERT_SHA
EOF

mkdir -p ./connectors/$CLIENT_NAME/keys/clients/
cp "$CLIENT_CERT" ./connectors/$CLIENT_NAME/keys/clients/${CLIENT_ID}.cert

cat ./connectors/$CLIENT_NAME/config/clients.yml | kubectl exec -n $KUBERNETES_NAMESPACE -i $OMEJDN_SERVER_POD_NAME -- sh -c 'cat >> /opt/config/clients.yml'

kubectl cp -n $KUBERNETES_NAMESPACE ./connectors/$CLIENT_NAME/keys/ $OMEJDN_SERVER_POD_NAME:/opt/

openssl pkcs12 -export -in ./connectors/$CLIENT_NAME/keys/$CLIENT_NAME.cert -inkey ./connectors/$CLIENT_NAME/keys/$CLIENT_NAME.key -out ./connectors/$CLIENT_NAME/keystore.p12 -passout pass:$KEYSTORE_PASSWORD

echo "Successfully registered client $CLIENT_NAME"
echo "Client ID: $CLIENT_ID"
echo "Keystore base64 encoded: $(base64 ./connectors/$CLIENT_NAME/keystore.p12 -w 0)"