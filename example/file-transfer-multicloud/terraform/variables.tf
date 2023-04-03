variable "location" {
  description = "geographic location of the Azure resources"
  default     = "westeurope"
  type        = string
}


variable "environment" {
  description = "identifying string that is used in all azure resources"
}
