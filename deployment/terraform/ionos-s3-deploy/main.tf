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

variable "image_repository" {
  type = string
  default = "ghcr.io/digital-ecosystems/connector"
}

variable "image_tag" {
  type = string
  default = ""
}

variable "pg_host" {
  type = string
  default = "localhost"
}

variable "pg_port" {
  type = number
  default = 5432
}

variable "pg_database" {
  type = string
  default = "postgres"
}

variable "pg_username" {
  type = string
  default = "postgres"
}

variable "pg_password" {
  type = string
  default = "postgres"
}

variable "s3_endpoint_region" {}
variable "ionos_token" {}

variable "vault_name" {
  default = "vault"
}


locals {
  vault_token = fileexists("../vault-init/vault-tokens.json") ? "${jsondecode(file("../vault-init/vault-tokens.json")).auth.client_token}" : ""
}

resource "helm_release" "edc-ionos-s3" {
  name       = "edc-ionos-s3"

  repository = "../../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace
  create_namespace = true

  set {
    name  = "edc.vault.hashicorp.token"
    value = local.vault_token
  }

  values = [
    "${file("../../helm/edc-ionos-s3/values.yaml")}",
  ]

  set {
    name  = "edc.vault.hashicorp.url"
    value = "http://${var.vault_name}:8200"
  }

  set {
    name  = "edc.ionos.endpoint.region"
    value = var.s3_endpoint_region
  }

  set {
    name  = "edc.ionos.token"
    value = var.ionos_token
  }

  set {
    name  = "ids.webhook.address"
    value = var.ids_webhook_address
  }

  set {
    name  = "edc.postgresql.host"
    value = var.pg_host
  }

  set {
    name  = "edc.postgresql.database"
    value = var.pg_database
  }

  set {
    name  = "edc.postgresql.port"
    value = var.pg_port
  }

  set {
    name  = "edc.postgresql.username"
    value = var.pg_username
  }

  set {
    name  = "edc.postgresql.password"
    value = var.pg_password
  }

  set {
    name = "image.repository"
    value = var.image_repository
  }

  set {
    name = "image.tag"
    value = var.image_tag
  }
}