echo "Namespace: $TF_VAR_namespace"

# Get the connector address
CONNECTOR_ADDRESS=$(kubectl --kubeconfig=$TF_VAR_kubeconfig get svc -n $TF_VAR_namespace edc-ionos-s3 -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Check if the connector address is empty
if [ -z "$CONNECTOR_ADDRESS" ]; then
    echo "Connector address is empty"
    exit 1
fi

# Change public address in the config.properties in the configmap
kubectl --kubeconfig=$TF_VAR_kubeconfig -n $TF_VAR_namespace get configmap edc-ionos-s3-config -o yaml | sed "s/edc.dsp.callback.address=.*/edc.dsp.callback.address=http:\/\/$CONNECTOR_ADDRESS:8281\/protocol/g" | kubectl --kubeconfig=$TF_VAR_kubeconfig apply -f -

kubectl --kubeconfig=$TF_VAR_kubeconfig -n $TF_VAR_namespace get configmap edc-ionos-s3-config -o yaml | sed "s/edc.dataplane.token.validation.endpoint=.*/edc.dataplane.token.validation.endpoint=http:\/\/$CONNECTOR_ADDRESS:8283\/control\/token/g" | kubectl --kubeconfig=$TF_VAR_kubeconfig apply -f -

kubectl --kubeconfig=$TF_VAR_kubeconfig -n $TF_VAR_namespace get configmap edc-ionos-s3-config -o yaml | sed "s/edc.dataplane.api.public.baseurl=.*/edc.dataplane.api.public.baseurl=http:\/\/$CONNECTOR_ADDRESS:8282\/public/g" | kubectl --kubeconfig=$TF_VAR_kubeconfig apply -f -

# Restart the pods
kubectl --kubeconfig=$TF_VAR_kubeconfig -n $TF_VAR_namespace delete pod -l app.kubernetes.io/name=edc-ionos-s3