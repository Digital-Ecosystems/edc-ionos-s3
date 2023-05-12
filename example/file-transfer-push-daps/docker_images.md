### Manual build and push of the docker images

Follow the `Building and Running` section of the previous [readme](../../README.md).

### Create registry and token
In order to to build and push the docker images, you will need to follow the steps below:
(We will use the [DCD](https://dcd.ionos.com)
1) Create a Container Registry: access the `Containers/Container Registry` option and create a new registry;
2) Create a token: access the `Containers/Container Registry/<REGISTRY_NAME>/Tokens` and click on `Add` button. Set the `Name`, and add `push` and `pull` permissions on the repository. Create the token, and store the username and password;

### Build the docker images
```bash
docker build -t example.cr.de-fra.ionos.com/edc-ionos-s3:consumer consumer
docker build -t example.cr.de-fra.ionos.com/edc-ionos-s3:provider provider
```

### Push the docker images
```bash
docker push example.cr.de-fra.ionos.com/edc-ionos-s3:consumer
docker push example.cr.de-fra.ionos.com/edc-ionos-s3:provider
```

