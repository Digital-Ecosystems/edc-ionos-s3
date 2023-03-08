variable "namespace" {
  default = "edc-ionos-s3"
}

# install helm vault
resource "helm_release" "vault" {
  name       = "vault"

  repository = "../helm"
  chart      = "vault"

  namespace = "${var.namespace}"
  create_namespace = true
}