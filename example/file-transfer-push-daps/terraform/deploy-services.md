## deploy-services.sh documentation

The script automates the deployment of the example. It includes several Terraform scripts.

It takes one argument as input, which specifies the Terraform command to be run (`apply`, `destroy`, etc.). If no argument is passed, the default value is `apply`. The script then checks for several environment variables that must be set for the Terraform scripts to run correctly.

The environment variables that the script checks for are:

- `TF_VAR_kubeconfig`
- `KUBECONFIG`
- `TF_VAR_daps_url`
- `TF_VAR_edc_file_transfer_bucket_name`
- `TF_VAR_s3_access_key`
- `TF_VAR_s3_secret_key`
- `TF_VAR_ionos_token`
- `TF_VAR_s3_endpoint`

If any of these environment variables are undefined, the script stops execution and outputs an error message indicating which variable is undefined.

If `TF_VAR_registry_name` is undefined, the script generates a random name for the registry and exports the `TF_VAR_registry_name` environment variables.

Then executes the following Terraform modules in order:

1. `build-project`
2. `build-and-push-docker-images`
3. `create-daps-clients`
4. `vault-deploy`
5. `vault-init`
6. `ionos-s3-deploy`
7. `configure-ionos-s3-webhook-address`

Each of the Terraform scripts is initialized and then executed with the Terraform command specified as input or the default value (`apply`) if no input was given. 

Note that the `-auto-approve` flag is passed to each Terraform command, indicating that the command should be executed without prompting for confirmation.

If any of the Terraform scripts fail, the script stops execution and outputs an error message.