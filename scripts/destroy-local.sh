#!/usr/bin/env bash
# Remove o cluster kind local (aplicação, PostgreSQL e dados).
#
# Uso: ./scripts/destroy-local.sh

set -euo pipefail

CLUSTER="oficina-local"

if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  kind delete cluster --name "$CLUSTER"
else
  echo "Cluster kind $CLUSTER não encontrado. Nada a remover."
fi
