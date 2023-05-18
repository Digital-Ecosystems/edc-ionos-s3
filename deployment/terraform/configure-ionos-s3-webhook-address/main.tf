resource "null_resource" "configure-webhook-address" {
  provisioner "local-exec" {
    command = "./webhook-addresses.sh"
    interpreter = ["bash", "-c"]
  }
}
