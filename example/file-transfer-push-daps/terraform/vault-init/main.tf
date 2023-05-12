resource "null_resource" "vault-init" {
  provisioner "local-exec" {
    command = "${path.module}/vault-init.sh"
    interpreter = ["bash", "-c"]
  }
}