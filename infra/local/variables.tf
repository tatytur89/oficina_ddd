variable "cluster_name" {
  description = "Nome exclusivo do cluster local gerenciado por esta raiz."
  type        = string
  default     = "oficina-local"
  validation {
    condition     = can(regex("^oficina-local(-[a-z0-9]+)*$", var.cluster_name)) && length(var.cluster_name) <= 50
    error_message = "Use oficina-local ou um nome com esse prefixo, ate 50 caracteres."
  }
}

variable "http_port" {
  description = "Porta HTTP reservada em 127.0.0.1 para o futuro Ingress."
  type        = number
  default     = 8090
  validation {
    condition     = var.http_port >= 1024 && var.http_port <= 65535 && floor(var.http_port) == var.http_port
    error_message = "Use uma porta inteira entre 1024 e 65535."
  }
}

variable "https_port" {
  description = "Porta HTTPS reservada em 127.0.0.1 para o futuro Ingress."
  type        = number
  default     = 8443
  validation {
    condition     = var.https_port >= 1024 && var.https_port <= 65535 && floor(var.https_port) == var.https_port
    error_message = "Use uma porta inteira entre 1024 e 65535."
  }
}
