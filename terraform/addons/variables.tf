variable "aws_region" {
  description = "Região AWS onde o cluster foi criado."
  type        = string
}

variable "cluster_name" {
  description = "Nome do cluster EKS criado pela configuração principal."
  type        = string
}

variable "vpc_id" {
  description = "ID da VPC do cluster."
  type        = string
}

variable "controller_role_arn" {
  description = "ARN da role IRSA criada pela configuração principal."
  type        = string
}
