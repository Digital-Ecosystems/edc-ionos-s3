resource "null_resource" "build-project" {
  provisioner "local-exec" {
    command = "cd ../../../../ && ./gradlew clean build"
    interpreter = ["bash", "-c"]
  }
}