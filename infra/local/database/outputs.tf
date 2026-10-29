output "namespace" {
  value = var.namespace
}
output "service_host" {
  value = "postgres.${var.namespace}.svc.cluster.local"
}
output "migration_job_name" {
  value = kubernetes_job_v1.schema.metadata[0].name
}
