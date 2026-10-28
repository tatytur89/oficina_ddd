variable "aws_region" {
  description = "Região AWS onde a infraestrutura será criada."
  type        = string
  default     = "sa-east-1"
}

variable "project_name" {
  description = "Prefixo para os recursos AWS."
  type        = string
  default     = "oficina-ddd"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{0,29}$", var.project_name))
    error_message = "Use um nome minúsculo iniciado por letra, com até 30 letras, números ou hífens."
  }
}

variable "cluster_version" {
  description = "Versão Kubernetes do EKS."
  type        = string
  default     = "1.34"
}

variable "cluster_admin_cidrs" {
  description = "CIDRs IPv4 autorizados a acessar a API pública do EKS. Informe seu IP como x.x.x.x/32."
  type        = list(string)

  validation {
    condition = length(var.cluster_admin_cidrs) > 0 && alltrue([
      for cidr in var.cluster_admin_cidrs :
      can(cidrhost(cidr, 0)) && !strcontains(cidr, ":") && cidr != "0.0.0.0/0"
    ])
    error_message = "Informe ao menos um CIDR IPv4 válido e restrito; 0.0.0.0/0 não é permitido."
  }
}

variable "node_instance_types" {
  description = "Tipos de instância para os nós gerenciados do EKS."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "node_min_size" {
  description = "Número mínimo de nós EKS."
  type        = number
  default     = 2
}

variable "node_desired_size" {
  description = "Número inicial de nós EKS."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Número máximo de nós EKS."
  type        = number
  default     = 3
}

variable "database_name" {
  description = "Nome do banco PostgreSQL da aplicação."
  type        = string
  default     = "oficina_db"
}

variable "database_username" {
  description = "Usuário administrador do PostgreSQL; a senha é gerenciada pelo RDS no Secrets Manager."
  type        = string
  default     = "oficina_user"
}

variable "database_instance_class" {
  description = "Classe da instância RDS PostgreSQL."
  type        = string
  default     = "db.t4g.micro"
}

variable "database_multi_az" {
  description = "Ativa standby Multi-AZ para o PostgreSQL. Aumenta a disponibilidade e o custo."
  type        = bool
  default     = false
}

variable "database_deletion_protection" {
  description = "Impede que o RDS seja removido acidentalmente."
  type        = bool
  default     = true
}
