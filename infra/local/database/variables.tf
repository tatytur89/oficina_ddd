variable "kubeconfig_path" {
  type        = string
  description = "Kubeconfig exclusivo do cluster local existente."
  default     = "../.generated/kubeconfig"
}
variable "context" {
  type    = string
  default = "kind-oficina-local"
  validation {
    condition     = can(regex("^kind-oficina-local(-[a-z0-9]+)*$", var.context))
    error_message = "Escolha explicitamente um contexto kind do laboratorio oficina-local."
  }
}
variable "namespace" {
  type    = string
  default = "oficina-local"
  validation {
    condition     = can(regex("^(oficina-local|oficina-db-test-[a-z0-9]+)$", var.namespace))
    error_message = "Use oficina-local ou namespace isolado oficina-db-test-<id>."
  }
}

variable "external_secrets" {
  type        = bool
  description = "Use an existing Kubernetes Secret populated outside Terraform."
  default     = false
}

variable "database_password" {
  type        = string
  sensitive   = true
  description = "Senha usada apenas quando external_secrets=false (por exemplo, no CI efêmero)."
  default     = null

  validation {
    condition     = var.external_secrets ? true : (var.database_password != null && length(var.database_password) >= 16)
    error_message = "Informe database_password com pelo menos 16 caracteres ou habilite external_secrets."
  }
}
