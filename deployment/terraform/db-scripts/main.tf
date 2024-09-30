resource "null_resource" "db-scripts" {
  provisioner "local-exec" {
    command = "${path.module}/db-scripts.sh"
    interpreter = ["bash", "-c"]
  }
}