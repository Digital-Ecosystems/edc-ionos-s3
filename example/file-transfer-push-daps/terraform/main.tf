terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "2.9.0"
    }
  }
}

variable "kubeconfig" {
  type = string
}

variable "namespace_vault" {
  type = string
  default = "edc-ionos-s3-vault"
}

variable "namespace_consumer" {
  type = string
  default = "edc-ionos-s3-consumer"
}

variable "namespace_provider" {
  type = string
  default = "edc-ionos-s3-provider"
}

provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

module "vault-deploy" {
  source = "./vault-deploy"

  namespace_vault = "${var.namespace_vault}"
}

module "vault-init" {
  source = "./vault-init"

  depends_on = [
    module.vault-deploy
  ]
}

module "ionos-s3-deploy" {
  source = "./ionos-s3-deploy"

  depends_on = [
    module.vault-init
  ]

  namespace_consumer = "${var.namespace_consumer}"
  namespace_provider = "${var.namespace_provider}"
}