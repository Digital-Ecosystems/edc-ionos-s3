# Set the provider and consumer addresses
PROVIDER_ADDRESS=$(kubectl get svc -n edc-ionos-s3-provider edc-ionos-s3-provider -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
CONSUMER_ADDRESS=$(kubectl get svc -n edc-ionos-s3-consumer edc-ionos-s3-consumer -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Change the ids.webhook.address in the config.properties in the configmap
kubectl -n edc-ionos-s3-provider get configmap edc-ionos-s3-provider-config -o yaml | sed "s/ids.webhook.address=.*/ids.webhook.address=http:\/\/$PROVIDER_ADDRESS:8282/g" | kubectl apply -f -
kubectl -n edc-ionos-s3-consumer get configmap edc-ionos-s3-consumer-config -o yaml | sed "s/ids.webhook.address=.*/ids.webhook.address=http:\/\/$CONSUMER_ADDRESS:8282/g" | kubectl apply -f -

# Restart the pods
kubectl -n edc-ionos-s3-provider delete pod -l app.kubernetes.io/name=edc-ionos-s3
kubectl -n edc-ionos-s3-consumer delete pod -l app.kubernetes.io/name=edc-ionos-s3