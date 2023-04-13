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

variable "s3_access_key" {
  type = string
}

variable "s3_secret_key" {
  type = string
}

variable "s3_endpoint" {
  type = string
}
variable "s3_token" {
  type = string
}

variable "namespace" {
  type = string
  default = "edc-ionos-s3"
}

provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

module "vault-deploy" {
  source = "./vault-deploy"

  namespace = "${var.namespace}"
}

module "vault-init" {
  source = "./vault-init"

  depends_on = [
    module.vault-deploy
  ]

  s3_access_key = "${var.s3_access_key}"
  s3_secret_key = "${var.s3_secret_key}"
  s3_endpoint = "${var.s3_endpoint}"
}

module "ionos-s3-deploy" {
  source = "./ionos-s3-deploy"

  depends_on = [
    module.vault-init
  ]

  namespace = "${var.namespace}"
}