variable "kubeconfig_path" {
  type        = string
  description = "Kubeconfig exclusivo do cluster kind local já criado."
  default     = "../.generated/kubeconfig"
}

variable "context" {
  type    = string
  default = "kind-oficina-local"
  validation {
    condition     = can(regex("^kind-oficina-local(-[a-z0-9]+)*$", var.context))
    error_message = "Escolha um contexto kind do laboratório oficina-local."
  }
}
