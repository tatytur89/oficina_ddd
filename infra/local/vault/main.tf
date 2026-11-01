resource "helm_release" "vault" {
  name             = "vault"
  namespace        = "vault-system"
  create_namespace = true
  repository       = "https://helm.releases.hashicorp.com"
  chart            = "vault"
  version          = "0.34.1"
  timeout          = 600
  wait             = false
  atomic           = false
  cleanup_on_fail  = true

  values = [yamlencode({
    injector = { enabled = false }
    server = {
      standalone = {
        enabled = true
        config  = <<-EOT
          ui = true
          disable_mlock = true
          storage "file" {
            path = "/vault/data"
          }
          listener "tcp" {
            address         = "[::]:8200"
            cluster_address = "[::]:8201"
            tls_disable     = 1
          }
          api_addr = "http://vault.vault-system.svc:8200"
        EOT
      }
      ha = { enabled = false }
      dataStorage = {
        enabled      = true
        size         = "1Gi"
        accessMode   = "ReadWriteOnce"
        mountPath    = "/vault/data"
        storageClass = null
      }
      service = {
        enabled    = true
        type       = "ClusterIP"
        port       = 8200
        targetPort = 8200
      }
      resources = {
        requests = { cpu = "100m", memory = "128Mi" }
        limits   = { cpu = "500m", memory = "512Mi" }
      }
    }
    ui = { enabled = true }
  })]

  lifecycle {
    precondition {
      condition     = local.local_target
      error_message = "O contexto deve apontar para a API kind local em https://127.0.0.1:<porta>."
    }
  }
}
