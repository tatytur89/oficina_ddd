locals {
  name = var.project_name

  tags = {
    Project     = var.project_name
    ManagedBy   = "Terraform"
    Environment = "production"
  }

  cluster_tag = {
    "kubernetes.io/cluster/${local.name}" = "shared"
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_rds_engine_version" "postgres" {
  engine                 = "postgres"
  parameter_group_family = "postgres16"
  version                = "16"
  latest                 = true
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.21.0"

  name = "${local.name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets  = ["10.0.0.0/24", "10.0.1.0/24"]
  private_subnets = ["10.0.10.0/24", "10.0.11.0/24"]
  database_subnets = [
    "10.0.20.0/24",
    "10.0.21.0/24"
  ]

  enable_nat_gateway     = true
  single_nat_gateway     = true
  one_nat_gateway_per_az = false
  enable_dns_hostnames   = true
  enable_dns_support     = true

  public_subnet_tags = merge(local.cluster_tag, {
    "kubernetes.io/role/elb" = "1"
  })

  private_subnet_tags = merge(local.cluster_tag, {
    "kubernetes.io/role/internal-elb" = "1"
  })

  tags = local.tags
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "20.37.1"

  cluster_name    = local.name
  cluster_version = var.cluster_version

  cluster_endpoint_private_access          = true
  cluster_endpoint_public_access           = true
  cluster_endpoint_public_access_cidrs     = var.cluster_admin_cidrs
  enable_irsa                              = true
  enable_cluster_creator_admin_permissions = true
  authentication_mode                      = "API_AND_CONFIG_MAP"

  vpc_id                   = module.vpc.vpc_id
  subnet_ids               = module.vpc.private_subnets
  control_plane_subnet_ids = module.vpc.private_subnets

  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      before_compute = true
      most_recent    = true
    }
  }

  eks_managed_node_groups = {
    application = {
      name           = "${local.name}-nodes"
      instance_types = var.node_instance_types
      min_size       = var.node_min_size
      max_size       = var.node_max_size
      desired_size   = var.node_desired_size
      capacity_type  = "ON_DEMAND"
    }
  }

  cloudwatch_log_group_retention_in_days = 30
  tags                                   = local.tags
}

resource "aws_security_group" "database" {
  name_prefix = "${local.name}-postgres-"
  description = "Allow PostgreSQL only from the EKS worker nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_db_subnet_group" "database" {
  name       = "${local.name}-postgres"
  subnet_ids = module.vpc.database_subnets

  tags = local.tags
}

module "database" {
  source  = "terraform-aws-modules/rds/aws"
  version = "6.12.0"

  identifier           = "${local.name}-postgres"
  engine               = "postgres"
  engine_version       = data.aws_rds_engine_version.postgres.version
  family               = "postgres16"
  major_engine_version = "16"
  instance_class       = var.database_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.database_name
  username = var.database_username
  port     = 5432

  manage_master_user_password = true
  create_db_subnet_group      = false
  db_subnet_group_name        = aws_db_subnet_group.database.name
  vpc_security_group_ids      = [aws_security_group.database.id]
  publicly_accessible         = false

  multi_az                         = var.database_multi_az
  backup_retention_period          = 7
  deletion_protection              = var.database_deletion_protection
  skip_final_snapshot              = var.database_skip_final_snapshot
  final_snapshot_identifier_prefix = "${local.name}-final"
  copy_tags_to_snapshot            = true

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = local.tags
}

resource "aws_iam_role" "aws_load_balancer_controller" {
  name = "${local.name}-aws-lb-controller"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = module.eks.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:aud" = "sts.amazonaws.com"
          "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
        }
      }
    }]
  })

  tags = local.tags
}

resource "aws_iam_policy" "aws_load_balancer_controller" {
  name        = "${local.name}-aws-lb-controller"
  description = "Permissions for the AWS Load Balancer Controller"
  policy      = file("${path.module}/aws-load-balancer-controller-policy.json")
  tags        = local.tags
}

resource "aws_iam_role_policy_attachment" "aws_load_balancer_controller" {
  role       = aws_iam_role.aws_load_balancer_controller.name
  policy_arn = aws_iam_policy.aws_load_balancer_controller.arn
}
