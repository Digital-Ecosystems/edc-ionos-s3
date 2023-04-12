variable "namespace" {
  default = "edc-ionos-s3"
}

resource "helm_release" "edc-ionos-s3" {
  name       = "edc-ionos-s3"

  repository = "../helm"
  chart      = "edc-ionos-s3"

  namespace = var.namespace
  create_namespace = true

  set {
    name  = "edc.vault.hashicorp.token"
    value = "${jsondecode(file("./vault-keys.json")).root_token}"
  }

  values = [
    "${file("../helm/edc-ionos-s3/values.yaml")}",
  ]
}