# general

variable "prefix" {
  type = string
  validation {
    condition = (
    length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env" {
  type = string
}

variable "env_short" {
  type = string
  validation {
    condition = (
    length(var.env_short) == 1
    )
    error_message = "Length must be 1 chars."
  }
}

variable "location_short" {
  type = string
  validation {
    condition = (
    length(var.location_short) == 3
    )
    error_message = "Length must be 3 chars."
  }
  description = "One of weu, neu"
}

variable "tags" {
  type    = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

variable "domain" {
  type = string
  validation {
    condition = (
    length(var.domain) <= 12
    )
    error_message = "Max length is 12 chars."
  }
}

variable "location" {
  type = string
}

variable "instance" {
  type        = string
  description = "Identifies the instance"
  default     = "dev"
}

#apim
variable "apim_dns_zone_prefix" {
  type = string
}

variable "external_domain" {
  type = string
}

variable "hostname" {
  type = string
}
