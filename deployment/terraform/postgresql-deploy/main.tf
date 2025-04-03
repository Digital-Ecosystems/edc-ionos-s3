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

variable "pg_username" {
  type = string
  default = "postgres"
}

variable "pg_password" {
  type = string
  default = "postgres"
}

variable "pg_database" {
  type = string
  default = "postgres"
}

resource "helm_release" "postgresql" {
  name       = "postgresql"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  version    = "15.5.38"

  namespace = var.namespace

  set {
    name  = "global.postgresql.auth.username"
    value = var.pg_username
  }

  set {
    name  = "global.postgresql.auth.password"
    value = var.pg_password
  }

  set {
    name  = "global.postgresql.auth.database"
    value = var.pg_database
  }
}