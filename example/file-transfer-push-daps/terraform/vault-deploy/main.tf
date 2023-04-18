provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

variable "kubeconfig" {
  type = string
}

variable "namespace_vault" {
  default = "edc-ionos-s3-vault"
}

# install helm vault
resource "helm_release" "vault" {
  name       = "vault"

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version = "v0.19.0"

  namespace = var.namespace_vault
  create_namespace = true
}