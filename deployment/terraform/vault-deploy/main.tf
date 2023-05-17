provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

provider "kubernetes" {
  config_path = "${var.kubeconfig}"
}

variable "kubeconfig" {
  type = string
}

variable "namespace" {
  default = "edc-ionos-s3"
}

resource "kubernetes_namespace" "edc-namespace" {
  metadata {
    name = var.namespace
  }
}

# install helm vault
resource "helm_release" "vault" {
  name       = "vault"

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version = "v0.19.0"

  namespace = var.namespace
  create_namespace = true
}