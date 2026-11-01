output "cluster_name" {
  value = terraform_data.cluster.output.name
}
output "context" {
  value = "kind-${terraform_data.cluster.output.name}"
}
output "kubeconfig_path" {
  value = local.kubeconfig
}
output "http_url" {
  description = "Endereco reservado; depende da instalacao posterior do Ingress."
  value       = "http://127.0.0.1:${var.http_port}"
}
output "https_url" {
  description = "Endereco reservado; depende da instalacao posterior de Ingress e TLS."
  value       = "https://127.0.0.1:${var.https_port}"
}
