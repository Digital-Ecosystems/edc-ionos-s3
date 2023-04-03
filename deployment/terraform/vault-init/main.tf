variable "s3_access_key" {
  type = string
}

variable "s3_secret_key" {
  type = string
}

variable "s3_endpoint" {
  type = string
}

resource "null_resource" "vault-init" {
  provisioner "local-exec" {
    command = "${path.module}/vault-init.sh"
    interpreter = ["bash", "-c"]
  }
}