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

variable "persistence_type" {
  type = string
  default = "None"
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

variable "s3_access_key" {}
variable "s3_secret_key" {}
variable "s3_endpoint" {}
variable "ionos_token" {}

variable "vaultname" {
  default = "vault"
}

locals {
  root_token = fileexists("../vault-init/vault-keys.json") ? "${jsondecode(file("../vault-init/vault-keys.json")).root_token}" : ""
}

resource "helm_release" "edc-ionos-s3" {
  name       = "edc-ionos-s3"

  repository = "../../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace
  create_namespace = true

  set {
    name  = "edc.vault.hashicorp.token"
    value = "${jsondecode(file("../vault-init/vault-keys.json")).root_token}"
  }

  values = [
    "${file("../../helm/edc-ionos-s3/values.yaml")}",
  ]

  set {
    name  = "edc.vault.hashicorp.url"
    value = "http://${var.vaultname}:8200"
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
}