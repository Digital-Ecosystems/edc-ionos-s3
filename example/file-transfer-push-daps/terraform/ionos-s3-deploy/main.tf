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
  default = "provider"
  type = string
}

variable "consumer_ids_webhook_address" {
  default = "http://localhost:8282"
}

variable "daps_url" {}
variable "container_registry_url" {}
variable "container_repository_username" {
  default = "edc-ionos-s3"
}

# Consumer
variable "consumer_edc_keystore_password" {
  default = "consumer"
}
variable "consumer_image_tag" {
  default = "consumer"
}

# Provider
variable "provider_ids_webhook_address" {
  default = "http://localhost:8282"
}
variable "provider_edc_keystore_password" {
  default = "provider"
}
variable "provider_image_tag" {
  default = "provider"
}

variable "s3_access_key" {}
variable "s3_secret_key" {}
variable "s3_endpoint" {}
variable "ionos_token" {}

locals {
  root_token = "${jsondecode(file("../vault-init/vault-keys.json")).root_token}"

  consumer_edc_oauth_clientId = "${yamldecode(file("../create-daps-clients/connectors/consumer/config/clients.yml"))[0].client_id}"
  consumer_edc_keystore = "${filebase64("../create-daps-clients/connectors/consumer/keystore.p12")}"

  provider_edc_oauth_clientId = "${yamldecode(file("../create-daps-clients/connectors/provider/config/clients.yml"))[0].client_id}"
  provider_edc_keystore = "${filebase64("../create-daps-clients/connectors/provider/keystore.p12")}"
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
    value = var.ionos_token
  }

  set {
    name  = "ids.webhook.address"
    value = var.consumer_ids_webhook_address
  }

  set {
    name  = "edc.keystore"
    value = local.consumer_edc_keystore
  }

  set {
    name  = "edc.keystorePassword"
    value = var.consumer_edc_keystore_password
  }

  set {
    name  = "edc.oauth.tokenUrl"
    value = "${var.daps_url}/auth/token"
  }

  set {
    name  = "edc.oauth.clientId"
    value = local.consumer_edc_oauth_clientId
  }

  set {
    name  = "edc.oauth.providerJwksUrl"
    value = "${var.daps_url}/auth/jwks.json"
  }

  set {
    name  = "image.repository"
    value = "${var.container_registry_url}/edc-ionos-s3"
  }

  set {
    name  = "image.tag"
    value = var.consumer_image_tag
  }

  set {
    name  = "imagePullSecret.username"
    value = "${var.container_repository_username}"
  }

  set {
    name  = "imagePullSecret.password"
    value = "${file("../build-and-push-docker-images/registry_password.txt")}"
  }

  set {
    name  = "imagePullSecret.server"
    value = "${var.container_registry_url}"
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
    value = local.provider_edc_keystore
  }

  set {
    name  = "edc.keystorePassword"
    value = var.provider_edc_keystore_password
  }

  set {
    name  = "edc.oauth.tokenUrl"
    value = "${var.daps_url}/auth/token"
  }

  set {
    name  = "edc.oauth.clientId"
    value = local.provider_edc_oauth_clientId
  }

  set {
    name  = "edc.oauth.providerJwksUrl"
    value = "${var.daps_url}/auth/jwks.json"
  }

  set {
    name  = "image.repository"
    value = "${var.container_registry_url}/edc-ionos-s3"
  }

  set {
    name  = "image.tag"
    value = var.provider_image_tag
  }

  set {
    name  = "imagePullSecret.username"
    value = "${var.container_repository_username}"
  }

  set {
    name  = "imagePullSecret.password"
    value = "${file("../build-and-push-docker-images/registry_password.txt")}"
  }

  set {
    name  = "imagePullSecret.server"
    value = "${var.container_registry_url}"
  }

  set {
    name  = "service.type"
    value = "LoadBalancer"
  }

}
