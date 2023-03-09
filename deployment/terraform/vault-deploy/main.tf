variable "namespace" {
  default = "edc-ionos-s3"
}

# install helm vault
resource "helm_release" "vault" {
  name       = "vault"

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version = "v0.19.0"

  namespace = "${var.namespace}"
  create_namespace = true
}