output "cluster_name" {
  description = "Nome do cluster EKS."
  value       = module.eks.cluster_name
}

output "configure_kubectl" {
  description = "Comando para configurar o kubectl."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "database_endpoint" {
  description = "Endpoint privado do RDS PostgreSQL, sem a porta."
  value       = module.database.db_instance_address
}

output "database_name" {
  description = "Nome do banco da aplicação."
  value       = var.database_name
}

output "database_username" {
  description = "Usuário PostgreSQL gerenciado pelo RDS."
  value       = var.database_username
}

output "database_master_secret_arn" {
  description = "ARN do segredo gerenciado pelo RDS no AWS Secrets Manager."
  value       = module.database.db_instance_master_user_secret_arn
}

output "public_subnets" {
  description = "Subnets públicas usadas pelo ALB."
  value       = module.vpc.public_subnets
}

output "aws_region" {
  description = "Região em que a infraestrutura foi provisionada."
  value       = var.aws_region
}

output "vpc_id" {
  description = "ID da VPC criada."
  value       = module.vpc.vpc_id
}

output "aws_load_balancer_controller_role_arn" {
  description = "ARN da role IRSA do AWS Load Balancer Controller."
  value       = aws_iam_role.aws_load_balancer_controller.arn
}
