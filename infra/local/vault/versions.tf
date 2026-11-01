terraform {
  required_version = ">= 1.5.0"

  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "2.17.0"
    }
  }

  backend "local" {
    path = "terraform.tfstate"
  }
}

locals {
  kubeconfig       = yamldecode(file(pathexpand(var.kubeconfig_path)))
  selected_context = one([for context in local.kubeconfig.contexts : context.context if context.name == var.context])
  selected_cluster = one([for cluster in local.kubeconfig.clusters : cluster.cluster if cluster.name == local.selected_context.cluster])
  local_target     = can(regex("^https://127\\.0\\.0\\.1:[0-9]+$", local.selected_cluster.server))
}

provider "helm" {
  kubernetes {
    config_path    = pathexpand(var.kubeconfig_path)
    config_context = var.context
    host           = local.local_target ? local.selected_cluster.server : "https://127.0.0.1:1"
  }
}
