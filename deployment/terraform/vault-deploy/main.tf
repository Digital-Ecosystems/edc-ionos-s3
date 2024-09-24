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

variable "vaultname" {
  default = "vault"
}

resource "kubernetes_namespace" "edc-namespace" {
  metadata {
    name = var.namespace
  }
}

# install helm vault
resource "helm_release" "vault" {
  name       = var.vaultname

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version    = "v0.28.1"

  namespace = var.namespace
  create_namespace = true
}