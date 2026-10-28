#!/usr/bin/env bash
# Sobe a aplicação num cluster Kubernetes local (kind) com PostgreSQL no próprio cluster.
#
# Passos:
#   1. Cria o cluster kind "oficina-local", se ainda não existir
#   2. Instala o metrics-server (necessário para o HPA)
#   3. Constrói a imagem oficina-ddd:local e carrega no cluster
#   4. Aplica os manifests de k8s/local e espera o rollout
#
# Uso: ./scripts/deploy-local.sh
# Requer: docker, kind, kubectl e helm.
# Rode de novo após alterar o código para reconstruir e reimplantar a imagem.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER="oficina-local"
CONTEXT="kind-$CLUSTER"
NAMESPACE="oficina-api"
IMAGE="oficina-ddd:local"

for cmd in docker kind kubectl helm; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Comando '$cmd' não encontrado. Instale-o antes de continuar (veja o README)."
    exit 1
  fi
done

# 1. Cluster
if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  echo "==> Criando cluster kind $CLUSTER"
  kind create cluster --config "$ROOT_DIR/k8s/local/kind-config.yaml"
fi

# 2. metrics-server (kind usa certificados autoassinados no kubelet)
echo "==> Instalando metrics-server"
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/ >/dev/null 2>&1 || true
helm upgrade --install metrics-server metrics-server/metrics-server \
  --kube-context "$CONTEXT" --namespace kube-system \
  --set 'args={--kubelet-insecure-tls}' --wait

# 3. Imagem
echo "==> Construindo imagem $IMAGE"
docker build -t "$IMAGE" --build-arg APP_COMMIT_SHA="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo local)" "$ROOT_DIR"
kind load docker-image "$IMAGE" --name "$CLUSTER"

# 4. Manifests
echo "==> Aplicando manifests"
kubectl --context "$CONTEXT" apply -k "$ROOT_DIR/k8s/local"
kubectl --context "$CONTEXT" rollout status -n "$NAMESPACE" statefulset/postgres --timeout=180s
# A tag da imagem não muda entre builds; o restart força os pods a usarem a imagem recém-carregada
kubectl --context "$CONTEXT" rollout restart -n "$NAMESPACE" deployment/oficina-api
kubectl --context "$CONTEXT" rollout status -n "$NAMESPACE" deployment/oficina-api --timeout=300s

echo
echo "API:     http://localhost:8090"
echo "Swagger: http://localhost:8090/swagger-ui/index.html"
