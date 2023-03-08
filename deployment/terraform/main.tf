terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "2.9.0"
    }
  }
}

provider "helm" {
  kubernetes {
    config_path = "./kubeconfig-ionos.yaml"
  }
}

variable "namespace" {
  default = "edc-ionos-s3"
}

resource "helm_release" "edc-ionos-s3" {
  name       = "edc-ionos-s3"

  repository = "../deployment/helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace
  create_namespace = true
}