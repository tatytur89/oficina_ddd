output "namespace" {
  value = helm_release.vault.namespace
}

output "service" {
  value = "vault.${helm_release.vault.namespace}.svc:8200"
}
