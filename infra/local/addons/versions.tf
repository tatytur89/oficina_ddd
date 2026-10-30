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

provider "helm" {
  kubernetes {
    config_path    = pathexpand(var.kubeconfig_path)
    config_context = var.context
    # Mesmo um refresh não deve contactar um endpoint externo se o arquivo foi trocado.
    host = local.local_target ? local.selected_cluster.server : "https://127.0.0.1:1"
  }
}
