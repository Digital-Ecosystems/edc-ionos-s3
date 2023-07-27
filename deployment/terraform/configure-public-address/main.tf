resource "null_resource" "configure-public-address" {
  provisioner "local-exec" {
    command = "./public-addresses.sh"
    interpreter = ["bash", "-c"]
  }
}
