variable "cluster_name" {
  type    = string
  default = "oficina-local-ci"
  validation {
    condition     = can(regex("^oficina-local-ci(-[a-z0-9]+)*$", var.cluster_name)) && length(var.cluster_name) <= 50
    error_message = "Use um nome exclusivo com prefixo oficina-local-ci."
  }
}

locals {
  root       = abspath(path.module)
  node_image = "kindest/node:v1.34.11@sha256:44e222ee2132dab25ff87301682f89eb82c7880ea3a1bf543bfe9708fd08d67d"
  config = {
    kind       = "Cluster"
    apiVersion = "kind.x-k8s.io/v1alpha4"
    networking = { apiServerAddress = "127.0.0.1", ipFamily = "ipv4" }
    nodes = [
      {
        role   = "control-plane"
        image  = local.node_image
        labels = { ingress-ready = "true" }
        extraPortMappings = [
          { containerPort = 80, hostPort = 8090, listenAddress = "127.0.0.1", protocol = "TCP" },
          { containerPort = 443, hostPort = 8443, listenAddress = "127.0.0.1", protocol = "TCP" }
        ]
      },
      { role = "worker", image = local.node_image }
    ]
  }
}

resource "terraform_data" "cluster" {
  input = {
    root   = local.root
    name   = var.cluster_name
    config = base64encode(jsonencode(local.config))
  }
  triggers_replace = [
    var.cluster_name, local.config,
    filesha256("${path.module}/scripts/manage-cluster.sh")
  ]

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = "bash \"$OFICINA_ROOT/scripts/manage-cluster.sh\" create"
    environment = {
      OFICINA_ROOT   = self.input.root
      OFICINA_NAME   = self.input.name
      OFICINA_CONFIG = self.input.config
    }
  }

  provisioner "local-exec" {
    when        = destroy
    interpreter = ["/bin/bash", "-c"]
    command     = "bash \"$OFICINA_ROOT/scripts/manage-cluster.sh\" destroy"
    environment = {
      OFICINA_ROOT = self.input.root
      OFICINA_NAME = self.input.name
    }
  }
}

output "cluster_name" {
  value = terraform_data.cluster.output.name
}

output "context" {
  value = "kind-${terraform_data.cluster.output.name}"
}

output "kubeconfig_path" {
  value = "${local.root}/.generated/kubeconfig"
}
