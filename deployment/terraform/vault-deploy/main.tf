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

variable "vault_name" {
  default = "vault"
}


resource "kubernetes_namespace" "edc-namespace" {
  metadata {
    name = var.namespace
  }
}

# install helm vault
resource "helm_release" "vault" {
  name       = var.vault_name

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version    = "v0.28.1"

  namespace = var.namespace
  create_namespace = true

  set {
    name  = "injector.enabled"
    value = "false"
  }
}