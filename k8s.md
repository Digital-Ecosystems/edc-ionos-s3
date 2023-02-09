# Running EDC with IONOS S3 Extension - Kubernetes

This document explain how to deploy an EDC with IONOS S3 Extension into a Kubernetes using IONOS's Cloud.

Note: at the time of this writing. EDC has still no deployment using Helm charts;

## Requirements
- IONOS Account;
- IONOS Running Kubernetes cluster (if you don't have it, take a look at [this](https://gist.github.com/bozakov/ae289c0f84dc88a75a6b426540dca29d));
- IONOS Registry service (if you don't have it, take a look at [this](https://github.com/paulocabrita-ionos/container-registry-ionos-howto));

## Docker image
After building the connector, we can now create the docker container image. Use the following snippet:
```bash
cd connector
docker build -t edc-ionos-s3 .
```

## Pushing the image
To push your image to your registry just do the following:

```bash
docker login -u="<SCOPE NAME>" -p="<PASSWORD GENERATED>" <YOUR REGISTRY NAME>.ionos.com
docker tag edc-ionos-s3:latest <YOUR REGISTRY NAME>.ionos.com/edc-ionos-s3:latest
docker push <YOUR REGISTRY NAME>.ionos.com/edc-ionos-s3:latest
```

## Deploying into the Kubernetes cluster
After having the docker image uploaded into your registry, you can now use it to deploy the connector. Follow the next steps:
- Create a secret: required by the Kubernetes to access your registry;
```bash
kubectl create secret docker-registry <SECRET NAME> --docker-server=<YOUR REGISTRY NAME>.ionos.com --docker-username=<YOUR USERNAME> --docker-password=<YOUR PASSWORD> --docker-email=<YOUR EMAIL>
```

- Deploying the connector: for that matter we will use a simple way of doing it;
```bash
kubectl run <App Name> --image=<YOUR REGISTRY NAME>.ionos.com/edc-ionos-s3 --overrides='{ "apiVersion": "v1", "spec": { "imagePullSecrets": [{"name": "<YOUR SECRET NAME>"}] } }'
```

## Accessing the Connector
Now, we can access our Connector. There is serveral ways to configure the deployed app for remote accessing. It depends of course on your context. For the sake of simplicity we will use the `port-forward` method.

```bash
kubectl port-forward edc-ionos-s3 8181:8181 --address='0.0.0.0'
```

Check if it's up and running by executing the health check:
```bash
curl -H "X-Api-Key: password" --fail http://localhost:8181/api/check/health
```

You will have a return message like the following:
```bash
{"componentResults":[{"failure":null,"component":null,"isHealthy":true}],"isSystemHealthy":true}
```
