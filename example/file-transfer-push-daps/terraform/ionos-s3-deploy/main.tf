provider "helm" {
  kubernetes {
    config_path = "${var.kubeconfig}"
  }
}

variable "kubeconfig" {
  type = string
}

variable "namespace_consumer" {
  default = "edc-ionos-s3-consumer"
}

variable "namespace_provider" {
  default = "edc-ionos-s3-provider"
}

variable "edc_file_transfer_blob_name" {
  default = "device1-data.csv"
  type = string
}

variable "edc_file_transfer_bucket_name" {
  default = "company1"
  type = string
}

variable "consumer_ids_webhook_address" {
  default = "http://localhost:8282"
}
variable "consumer_edc_keystore" {}
variable "consumer_edc_keystorePassword" {}
variable "consumer_edc_oauth_tokenUrl" {}
variable "consumer_edc_oauth_clientId" {}
variable "consumer_edc_oauth_providerJwksUrl" {}
variable "consumer_image_repository" {}
variable "consumer_image_tag" {}
variable "consumer_image_pull_secret_docker_config_json" {}

variable "provider_ids_webhook_address" {
  default = "http://localhost:8282"
}
variable "provider_edc_keystore" {}
variable "provider_edc_keystorePassword" {}
variable "provider_edc_oauth_tokenUrl" {}
variable "provider_edc_oauth_clientId" {}
variable "provider_edc_oauth_providerJwksUrl" {}
variable "provider_image_repository" {}
variable "provider_image_tag" {}
variable "provider_image_pull_secret_docker_config_json" {}

variable "s3_access_key" {}
variable "s3_secret_key" {}
variable "s3_endpoint" {}
variable "s3_token" {}

locals {
  root_token = fileexists("../vault-init/vault-keys.json") ? "${jsondecode(file("../vault-init/vault-keys.json")).root_token}" : ""
}

resource "helm_release" "edc-ionos-s3-consumer" {
  name       = "edc-ionos-s3-consumer"

  repository = "../../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace_consumer
  create_namespace = true

  values = [
    "${file("../../helm/edc-ionos-s3/values.yaml")}",
  ]

  set {
    name  = "edc.vault.hashicorp.url"
    value = "http://vault.edc-ionos-s3-vault.svc.cluster.local:8200"
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
    value = var.consumer_ids_webhook_address
  }

  set {
    name  = "edc.keystore"
    value = var.consumer_edc_keystore
  }

  set {
    name  = "edc.keystorePassword"
    value = var.consumer_edc_keystorePassword
  }

  set {
    name  = "edc.oauth.tokenUrl"
    value = var.consumer_edc_oauth_tokenUrl
  }

  set {
    name  = "edc.oauth.clientId"
    value = var.consumer_edc_oauth_clientId
  }

  set {
    name  = "edc.oauth.providerJwksUrl"
    value = var.consumer_edc_oauth_providerJwksUrl
  }

  set {
    name  = "image.repository"
    value = var.consumer_image_repository
  }

  set {
    name  = "image.tag"
    value = var.consumer_image_tag
  }

  set {
    name  = "imagePullSecretDockerConfigJson"
    value = var.consumer_image_pull_secret_docker_config_json
  }

  set {
    name  = "service.type"
    value = "LoadBalancer"
  }
}

resource "helm_release" "edc-ionos-s3-provider" {
  name       = "edc-ionos-s3-provider"

  repository = "../../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace_provider
  create_namespace = true

  values = [
    "${file("../../helm/edc-ionos-s3/values.yaml")}",
  ]

  set {
    name  = "edc.vault.hashicorp.url"
    value = "http://vault.edc-ionos-s3-vault.svc.cluster.local:8200"
  }

  set {
    name  = "edc.vault.hashicorp.token"
    value = local.root_token
  }

  set {
    name  = "edc.file.transfer.blob.name"
    value = var.edc_file_transfer_blob_name
  }

  set {
    name  = "edc.file.transfer.bucket.name"
    value = var.edc_file_transfer_bucket_name
  }

  set {
    name  = "edc.ionos.accessKey"
    value = var.s3_access_key
  }

  set {
    name  = "edc.ionos.secretKey"
    value = var.s3_secret_key
  }

  set {
    name  = "edc.ionos.endpoint"
    value = var.s3_endpoint
  }

  set {
    name  = "ids.webhook.address"
    value = var.provider_ids_webhook_address
  }

  set {
    name  = "edc.keystore"
    value = var.provider_edc_keystore
  }

  set {
    name  = "edc.keystorePassword"
    value = var.provider_edc_keystorePassword
  }

  set {
    name  = "edc.oauth.tokenUrl"
    value = var.provider_edc_oauth_tokenUrl
  }

  set {
    name  = "edc.oauth.clientId"
    value = var.provider_edc_oauth_clientId
  }

  set {
    name  = "edc.oauth.providerJwksUrl"
    value = var.provider_edc_oauth_providerJwksUrl
  }

  set {
    name  = "image.repository"
    value = var.provider_image_repository
  }

  set {
    name  = "image.tag"
    value = var.provider_image_tag
  }

  set {
    name  = "imagePullSecretDockerConfigJson"
    value = var.provider_image_pull_secret_docker_config_json
  }

  set {
    name  = "service.type"
    value = "LoadBalancer"
  }

}
