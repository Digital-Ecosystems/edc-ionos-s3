variable "namespace_consumer" {
  default = "edc-ionos-s3-consumer"
}

variable "namespace_provider" {
  default = "edc-ionos-s3-provider"
}

resource "helm_release" "edc-ionos-s3-consumer" {
  name       = "edc-ionos-s3-consumer"

  repository = "../../../deployment/helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace_consumer
  create_namespace = true

  set {
    name  = "edc.vault.hashicorp.token"
    value = "${jsondecode(file("./vault-keys.json")).root_token}"
  }

  values = [
    "${file("../consumer/values.yaml")}",
  ]
}

resource "helm_release" "edc-ionos-s3-provider" {
  name       = "edc-ionos-s3-provider"

  repository = "../../../deployment/helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace_provider
  create_namespace = true

  set {
    name  = "edc.vault.hashicorp.token"
    value = "${jsondecode(file("./vault-keys.json")).root_token}"
  }

  values = [
    "${file("../provider/values.yaml")}",
  ]
}