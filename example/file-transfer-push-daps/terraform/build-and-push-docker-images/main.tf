terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
    ionoscloud = {
      source  = "ionos-cloud/ionoscloud"
      version = "= 6.3.5"
    }
    kubernetes = {}
  }
}

provider "ionoscloud" {
  token  = var.ionos_token
}

variable ionos_token {
  type = string
}

variable "registry_name" {}

resource "docker_image" "consumer" {
  name = "consumer"
  build {
    context = "../../consumer"
    tag     = ["${var.registry_name}.cr.de-fra.ionos.com/edc-ionos-s3:consumer"]
  }
}

resource "docker_image" "provider" {
  name = "provider"
  build {
    context = "../../provider"
    tag     = ["${var.registry_name}.cr.de-fra.ionos.com/edc-ionos-s3:provider"]
  }
}

resource "ionoscloud_container_registry" "container_registry" {
  garbage_collection_schedule {
    days             = ["Monday", "Tuesday"]
    time             = "05:19:00+00:00"
  }
  location  = "de/fra"
  name      = "${var.registry_name}"
}

resource "ionoscloud_container_registry_token" "container_registry_token" {
  name                  = "edc-ionos-s3"
  scopes  {
    actions             = ["push", "pull"]
    name                = "edc-ionos-s3"
    type                = "repository"
  }
  status                = "enabled"
  registry_id           = ionoscloud_container_registry.container_registry.id
  save_password_to_file = "registry_password.txt"
}

resource "null_resource" "docker_push" {
    provisioner "local-exec" {
    command = <<-EOT

    attempts=0
    max_attempts=120
    interval=30

    while true; do
      attempts=$((attempts+1))
      docker login -u edc-ionos-s3 -p $(cat registry_password.txt) ${var.registry_name}.cr.de-fra.ionos.com && \
      docker push ${var.registry_name}.cr.de-fra.ionos.com/edc-ionos-s3:consumer && \
      docker push ${var.registry_name}.cr.de-fra.ionos.com/edc-ionos-s3:provider && \
      break

      if [ $attempts -eq $max_attempts ]; then
        echo "Maximum attempts reached. Exiting..."
        exit 1
      fi

      echo "Attempt $attempts failed. Retrying in $interval seconds..."
      sleep $interval
    done
    EOT
    }

    depends_on = [
      docker_image.consumer,
      docker_image.provider,
      ionoscloud_container_registry_token.container_registry_token,
      ionoscloud_container_registry.container_registry
    ]
}