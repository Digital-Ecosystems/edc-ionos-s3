variable "datacenter_name" {
  type    = string
  default = "darashev-test1"
}

variable "datacenter_location" {
  type    = string
  default = "de/txl"
}

variable "kubernetes_node_pool_name" {
  type    = string
  default = "pool2"
}

variable "private_lan_name" {
  type    = string
  default = "k8s-lan"
}


variable "pg_instances" {
  type    = number
  default = 1
}

variable "pg_cluster_cores" {
  type    = number
  default = 2
}

variable "pg_cluster_ram" {
  type    = number
  default = 2048
}

variable "pg_storage_size" {
  type    = number
  default = 2048
}

variable "pg_version" {
  type    = number
  default = 15
}

variable "pg_storage_type" {
  type    = string
  default = "HDD"
}

variable "pg_display_name" {
  type    = string
  default = "EDC Ionos Postgres"
}

variable "ionos_token" {
  type    = string
  default = ""
}

variable "pg_username" {
  type    = string
  default = "edc-ionos"
}

variable "pg_password" {
  type    = string
  default = "edc-ionos-pass"
}

terraform {
  required_providers {
    ionoscloud = {
      source  = "ionos-cloud/ionoscloud"
      version = "= 6.4.10"
    }
  }
}

provider "ionoscloud" {
  token = var.ionos_token
}

data "ionoscloud_datacenter" "postgresaas" {
  name     = var.datacenter_name
  location = var.datacenter_location
}

data "ionoscloud_lan" "postgresaas" {
  datacenter_id = data.ionoscloud_datacenter.postgresaas.id
  name          = var.private_lan_name
}

data "ionoscloud_k8s_cluster" "postgresaas" {
  name = ""
}

data "ionoscloud_k8s_node_pool" "postgresaas" {
  k8s_cluster_id = data.ionoscloud_k8s_cluster.postgresaas.id
  name           = var.kubernetes_node_pool_name
}

data "ionoscloud_k8s_node_pool_nodes" "postgresaas" {
  node_pool_id   = data.ionoscloud_k8s_node_pool.postgresaas.id
  k8s_cluster_id = data.ionoscloud_k8s_cluster.postgresaas.id
}

data "ionoscloud_nic" "node1privatenic" {
  datacenter_id   = data.ionoscloud_datacenter.postgresaas.id
  server_id       = data.ionoscloud_k8s_node_pool_nodes.postgresaas.nodes[1].id
  name            = "private-lan-1"
}

locals {
  prefix           = format("%s/%s", data.ionoscloud_nic.node1privatenic.ips[0], "24")
  database_ip      = cidrhost(local.prefix, 1)
  database_ip_cidr = format("%s/%s", local.database_ip, "24")
}

resource "ionoscloud_pg_cluster" "postgresaas" {
  postgres_version = var.pg_version
  instances        = var.pg_instances
  cores            = var.pg_cluster_cores
  ram              = var.pg_cluster_ram
  storage_size     = var.pg_storage_size
  storage_type     = var.pg_storage_type
  connections {
    datacenter_id = data.ionoscloud_datacenter.postgresaas.id
    lan_id        = data.ionoscloud_lan.postgresaas.id
    cidr          = local.database_ip_cidr
  }
  location     = data.ionoscloud_datacenter.postgresaas.location
  display_name = var.pg_display_name
  maintenance_window {
    day_of_the_week = "Sunday"
    time            = "09:00:00"
  }
  credentials {
    username = var.pg_username
    password = var.pg_password
  }
  synchronization_mode = "ASYNCHRONOUS"
}
