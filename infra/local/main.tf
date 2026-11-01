locals {
  root       = abspath(path.module)
  kubeconfig = "${local.root}/.generated/kubeconfig"
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
          { containerPort = 80, hostPort = var.http_port, listenAddress = "127.0.0.1", protocol = "TCP" },
          { containerPort = 443, hostPort = var.https_port, listenAddress = "127.0.0.1", protocol = "TCP" }
        ]
      },
      { role = "worker", image = local.node_image }
    ]
  }
}

resource "terraform_data" "cluster" {
  input = {
    root        = local.root
    name        = var.cluster_name
    config      = base64encode(jsonencode(local.config))
    config_hash = sha256(jsonencode(local.config))
  }
  triggers_replace = [
    var.cluster_name, local.config,
    filesha256("${path.module}/scripts/common.ps1"),
    filesha256("${path.module}/scripts/create.ps1"),
    filesha256("${path.module}/scripts/destroy.ps1")
  ]

  lifecycle {
    precondition {
      condition     = var.http_port != var.https_port
      error_message = "HTTP e HTTPS devem usar portas diferentes."
    }
  }

  provisioner "local-exec" {
    interpreter = ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "RemoteSigned", "-Command"]
    command     = "& (Join-Path $env:OFICINA_ROOT 'scripts/create.ps1')"
    environment = {
      OFICINA_ROOT        = self.input.root
      OFICINA_NAME        = self.input.name
      OFICINA_CONFIG      = self.input.config
      OFICINA_CONFIG_HASH = self.input.config_hash
    }
  }

  provisioner "local-exec" {
    when        = destroy
    interpreter = ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "RemoteSigned", "-Command"]
    command     = "& (Join-Path $env:OFICINA_ROOT 'scripts/destroy.ps1')"
    environment = {
      OFICINA_ROOT = self.input.root
      OFICINA_NAME = self.input.name
    }
  }
}
