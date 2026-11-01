output "ingress_class" {
  value = "traefik"
}

output "ingress_namespace" {
  value = helm_release.traefik.namespace
}

output "ingress_deployment" {
  value = "oficina-traefik"
}

output "metrics_namespace" {
  value = helm_release.metrics_server.namespace
}

output "metrics_deployment" {
  value = "oficina-metrics-server"
}
