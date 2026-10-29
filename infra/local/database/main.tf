locals {
  kubeconfig       = yamldecode(file(pathexpand(var.kubeconfig_path)))
  selected_context = one([for context in local.kubeconfig.contexts : context.context if context.name == var.context])
  selected_cluster = one([for cluster in local.kubeconfig.clusters : cluster.cluster if cluster.name == local.selected_context.cluster])
  image            = "postgres:18.4-alpine3.23@sha256:996d0920e4ff9df1fc19dacb904492f3c1ec0ec1cc338f0ad7123be7731c5f5e"
  migrations_dir   = "${path.module}/../../../src/main/resources/db/migration"
  migrations       = { for name in fileset(local.migrations_dir, "V*.sql") : name => replace(file("${local.migrations_dir}/${name}"), "\r\n", "\n") }
  runner           = replace(file("${path.module}/../../../database/migrate.sh"), "\r\n", "\n")
  revision         = substr(sha256(jsonencode({ sql = local.migrations, runner = local.runner })), 0, 12)
}

# Namespace lifecycle belongs to the application, not to this database root.
data "kubernetes_namespace_v1" "target" {
  metadata { name = var.namespace }
  lifecycle {
    precondition {
      condition     = can(regex("^https://127\\.0\\.0\\.1:[0-9]+$", local.selected_cluster.server))
      error_message = "O contexto escolhido deve apontar para a API kind local em 127.0.0.1."
    }
  }
}

resource "kubernetes_secret_v1" "postgres" {
  metadata {
    name      = "postgres-secret"
    namespace = data.kubernetes_namespace_v1.target.metadata[0].name
  }
  data = {
    POSTGRES_DB       = "oficina_db"
    POSTGRES_USER     = "oficina_user"
    POSTGRES_PASSWORD = var.database_password
  }
}

resource "kubernetes_service_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = var.namespace
  }
  spec {
    cluster_ip = "None"
    selector   = { app = "postgres" }
    port {
      name        = "postgres"
      port        = 5432
      target_port = "postgres"
    }
  }
  depends_on = [data.kubernetes_namespace_v1.target]
}

resource "kubernetes_stateful_set_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = var.namespace
  }
  spec {
    service_name = kubernetes_service_v1.postgres.metadata[0].name
    replicas     = 1
    selector { match_labels = { app = "postgres" } }
    persistent_volume_claim_retention_policy {
      when_deleted = "Retain"
      when_scaled  = "Retain"
    }
    template {
      metadata { labels = { app = "postgres" } }
      spec {
        automount_service_account_token  = false
        termination_grace_period_seconds = 30
        container {
          name              = "postgres"
          image             = local.image
          image_pull_policy = "IfNotPresent"
          port {
            name           = "postgres"
            container_port = 5432
          }
          env_from {
            secret_ref { name = kubernetes_secret_v1.postgres.metadata[0].name }
          }
          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/18/docker"
          }
          volume_mount {
            name       = "data"
            mount_path = "/var/lib/postgresql"
          }
          resources {
            requests = { cpu = "100m", memory = "128Mi" }
            limits   = { cpu = "500m", memory = "512Mi" }
          }
          startup_probe {
            exec { command = ["sh", "-c", "pg_isready -h 127.0.0.1 -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""] }
            period_seconds    = 3
            failure_threshold = 60
          }
          readiness_probe {
            exec { command = ["sh", "-c", "pg_isready -h 127.0.0.1 -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""] }
            period_seconds = 5
          }
          liveness_probe {
            exec { command = ["sh", "-c", "pg_isready -h 127.0.0.1 -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""] }
            period_seconds    = 10
            failure_threshold = 6
          }
        }
      }
    }
    volume_claim_template {
      metadata { name = "data" }
      spec {
        access_modes = ["ReadWriteOnce"]
        resources { requests = { storage = "1Gi" } }
      }
    }
  }
  timeouts {
    create = "8m"
    update = "8m"
  }
}

resource "kubernetes_config_map_v1" "schema" {
  metadata {
    name      = "oficina-schema-${local.revision}"
    namespace = var.namespace
  }
  data       = merge(local.migrations, { "migrate.sh" = local.runner })
  depends_on = [data.kubernetes_namespace_v1.target]
}

resource "kubernetes_job_v1" "schema" {
  lifecycle {
    replace_triggered_by = [kubernetes_secret_v1.postgres]
  }
  metadata {
    name      = "oficina-schema-${local.revision}"
    namespace = var.namespace
  }
  spec {
    backoff_limit           = 0
    active_deadline_seconds = 240
    template {
      metadata { labels = { app = "oficina-schema" } }
      spec {
        restart_policy                  = "Never"
        automount_service_account_token = false
        container {
          name    = "migrate"
          image   = local.image
          command = ["sh", "/migrations/migrate.sh"]
          env {
            name  = "PGHOST"
            value = "postgres"
          }
          env {
            name  = "PGPORT"
            value = "5432"
          }
          env {
            name  = "PGCONNECT_TIMEOUT"
            value = "10"
          }
          dynamic "env" {
            for_each = { PGDATABASE = "POSTGRES_DB", PGUSER = "POSTGRES_USER", PGPASSWORD = "POSTGRES_PASSWORD" }
            content {
              name = env.key
              value_from {
                secret_key_ref {
                  name = kubernetes_secret_v1.postgres.metadata[0].name
                  key  = env.value
                }
              }
            }
          }
          volume_mount {
            name       = "migrations"
            mount_path = "/migrations"
            read_only  = true
          }
          resources {
            requests = { cpu = "50m", memory = "32Mi" }
            limits   = { cpu = "300m", memory = "128Mi" }
          }
        }
        volume {
          name = "migrations"
          config_map { name = kubernetes_config_map_v1.schema.metadata[0].name }
        }
      }
    }
  }
  wait_for_completion = true
  timeouts { create = "5m" }
  depends_on = [kubernetes_stateful_set_v1.postgres]
}
