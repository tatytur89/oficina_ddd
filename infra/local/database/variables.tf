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
variable "database_password" {
  type        = string
  sensitive   = true
  description = "Senha local do PostgreSQL; deve corresponder ao Secret da aplicacao."
  validation {
    condition     = length(var.database_password) >= 16
    error_message = "Informe uma senha com pelo menos 16 caracteres."
  }
}
