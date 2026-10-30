locals {
  kubeconfig       = yamldecode(file(pathexpand(var.kubeconfig_path)))
  selected_context = one([for context in local.kubeconfig.contexts : context.context if context.name == var.context])
  selected_cluster = one([for cluster in local.kubeconfig.clusters : cluster.cluster if cluster.name == local.selected_context.cluster])
  local_target     = can(regex("^https://127\\.0\\.0\\.1:[0-9]+$", local.selected_cluster.server))
}

resource "helm_release" "traefik" {
  name             = "oficina-traefik"
  namespace        = "oficina-ingress"
  create_namespace = true
  repository       = "https://traefik.github.io/charts"
  chart            = "traefik"
  version          = "42.1.3"
  timeout          = 300
  wait             = true
  atomic           = true
  cleanup_on_fail  = true
  skip_crds        = true

  values = [yamlencode({
    fullnameOverride = "oficina-traefik"
    versionOverride  = "v3.7.16"
    image = {
      registry   = "docker.io"
      repository = "library/traefik"
      digest     = "sha256:1336e85aaed293bd63d005086e0dd013b04078b26aaeb1c18b26a2f928af7e65"
    }
    deployment = { replicas = 1 }
    # O único nó ingress-ready tem hostPorts exclusivos; surge durante upgrade bloquearia o rollout.
    updateStrategy = { type = "Recreate" }
    nodeSelector   = { ingress-ready = "true" }
    tolerations = [{
      key      = "node-role.kubernetes.io/control-plane"
      operator = "Exists"
      effect   = "NoSchedule"
    }]
    ingressClass = {
      enabled        = true
      name           = "traefik"
      isDefaultClass = false
    }
    providers = {
      kubernetesCRD     = { enabled = false }
      kubernetesGateway = { enabled = false }
      kubernetesIngress = {
        enabled          = true
        ingressClass     = "traefik"
        namespaces       = []
        labelSelector    = "app.kubernetes.io/part-of=oficina-local"
        publishedService = { enabled = false }
      }
    }
    gateway = { enabled = false }
    api     = { dashboard = false, insecure = false }
    ingressRoute = {
      dashboard   = { enabled = false }
      healthcheck = { enabled = false }
    }
    service = { spec = { type = "ClusterIP" } }
    ports = {
      web       = { port = 8000, hostPort = 80 }
      websecure = { port = 8443, hostPort = 443 }
    }
    metrics = { prometheus = null }
    resources = {
      requests = { cpu = "50m", memory = "64Mi" }
      limits   = { cpu = "250m", memory = "256Mi" }
    }
    accessLog = { enabled = false }
  })]

  lifecycle {
    precondition {
      condition     = local.local_target
      error_message = "O contexto deve apontar para a API kind local em https://127.0.0.1:<porta>."
    }
  }
}

resource "helm_release" "metrics_server" {
  name       = "oficina-metrics-server"
  namespace  = "kube-system"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = "3.14.0"
  timeout    = 300
  wait       = true
  atomic     = true

  values = [yamlencode({
    fullnameOverride = "oficina-metrics-server"
    # Chart 3.14.0 traz 0.9.0 por padrão; usa-se o patch 0.9.1 verificado, com digest fixo.
    image = {
      repository = "registry.k8s.io/metrics-server/metrics-server"
      tag        = "v0.9.1@sha256:9b0ba1926e1d3089673f45461c3c1416109dea33b3ba3434048defd380df329b"
    }
    # Somente neste laboratório: kubelets kind usam certificados não verificáveis pela CA esperada.
    args       = ["--kubelet-insecure-tls"]
    apiService = { insecureSkipTLSVerify = true }
    resources = {
      requests = { cpu = "100m", memory = "200Mi" }
      limits   = { cpu = "250m", memory = "300Mi" }
    }
  })]

  lifecycle {
    precondition {
      condition     = local.local_target
      error_message = "O contexto deve apontar para a API kind local em https://127.0.0.1:<porta>."
    }
  }
}
