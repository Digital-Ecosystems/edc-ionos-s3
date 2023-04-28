variable "provider_keystore_password" {
    type = string
    default = "provider"
}

variable "consumer_keystore_password" {
    type = string
    default = "consumer"
}

resource "null_resource" "create-consumer-daps-client" {
  provisioner "local-exec" {
    command = "./register-connector.sh consumer ${var.consumer_keystore_password}"
    interpreter = ["bash", "-c"]
  }
}

resource "null_resource" "create-provider-daps-client" {
  provisioner "local-exec" {
    command = "./register-connector.sh provider ${var.provider_keystore_password}"
    interpreter = ["bash", "-c"]
  }
}