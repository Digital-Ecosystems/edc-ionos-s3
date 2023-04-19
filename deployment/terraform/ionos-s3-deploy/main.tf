provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

variable "kubeconfig" {
  type = string
}

variable "namespace" {
  default = "edc-ionos-s3"
}

variable "ids_webhook_address" {
  default = "http://localhost:8282"
}

variable "image_repository" {}
variable "image_tag" {}

variable "s3_access_key" {}
variable "s3_secret_key" {}
variable "s3_endpoint" {}
variable "s3_token" {}

locals {
  root_token = fileexists("../vault-init/vault-keys.json") ? "${jsondecode(file("../vault-init/vault-keys.json")).root_token}" : ""
}

resource "helm_release" "edc-ionos-s3" {
  name       = "edc-ionos-s3"

  repository = "../../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace
  create_namespace = true

  values = [
    "${file("../../helm/edc-ionos-s3/values.yaml")}",
  ]

  set {
    name  = "edc.vault.hashicorp.url"
    value = "http://vault:8200"
  }

  set {
    name  = "edc.vault.hashicorp.token"
    value = local.root_token
  }

  set {
    name  = "edc.ionos.endpoint"
    value = var.s3_endpoint
  }

  set {
    name  = "edc.ionos.token"
    value = var.s3_token
  }

  set {
    name  = "ids.webhook.address"
    value = var.ids_webhook_address
  }

  set {
    name  = "image.repository"
    value = var.image_repository
  }

  set {
    name  = "image.tag"
    value = var.image_tag
  }

}