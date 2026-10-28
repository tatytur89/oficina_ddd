#!/usr/bin/env bash
# Destrói toda a infraestrutura AWS na ordem correta e confere se sobrou algo cobrando.
#
# Ordem:
#   1. Remove o Ingress e espera o ALB criado pelo controller ser apagado
#   2. Destrói o AWS Load Balancer Controller (infra/addons)
#   3. Desliga a proteção contra exclusão do RDS, se ainda estiver ativa
#   4. Destrói a infraestrutura principal (infra/)
#   5. Lista recursos remanescentes que geram custo
#
# Uso: ./scripts/destroy.sh
# Região e cluster vêm dos outputs do Terraform; se o estado já não existir,
# use AWS_REGION e CLUSTER_NAME (padrões: sa-east-1 e oficina-ddd).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TF_MAIN="$ROOT_DIR/infra"
TF_ADDONS="$ROOT_DIR/infra/addons"
NAMESPACE="oficina-api"

tf_output() {
  terraform -chdir="$TF_MAIN" output -raw "$1" 2>/dev/null || true
}

REGION="$(tf_output aws_region)"
CLUSTER="$(tf_output cluster_name)"
VPC_ID="$(tf_output vpc_id)"
CONTROLLER_ROLE="$(tf_output aws_load_balancer_controller_role_arn)"
REGION="${REGION:-${AWS_REGION:-sa-east-1}}"
CLUSTER="${CLUSTER:-${CLUSTER_NAME:-oficina-ddd}}"
DB_IDENTIFIER="${CLUSTER}-postgres"

echo "Região:  $REGION"
echo "Cluster: $CLUSTER"
echo
echo "Isto vai APAGAR o cluster EKS, o banco RDS (com todos os dados), a VPC e o ALB."
read -r -p "Digite o nome do cluster para confirmar: " CONFIRM
if [[ "$CONFIRM" != "$CLUSTER" ]]; then
  echo "Confirmação não confere. Nada foi alterado."
  exit 1
fi

# 1. Ingress / ALB
if aws eks describe-cluster --region "$REGION" --name "$CLUSTER" >/dev/null 2>&1; then
  echo
  echo "==> Removendo Ingress (o controller apaga o ALB)"
  aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" >/dev/null
  kubectl delete ingress --all -n "$NAMESPACE" --ignore-not-found --wait=true --timeout=180s || true

  echo "==> Aguardando o ALB ser removido"
  for _ in $(seq 1 30); do
    REMAINING="$(aws resourcegroupstaggingapi get-resources --region "$REGION" \
      --resource-type-filters elasticloadbalancing:loadbalancer \
      --tag-filters "Key=elbv2.k8s.aws/cluster,Values=$CLUSTER" \
      --query 'length(ResourceTagMappingList)' --output text)"
    [[ "$REMAINING" == "0" ]] && break
    sleep 10
  done
  if [[ "$REMAINING" != "0" ]]; then
    echo "O ALB ainda existe após 5 minutos. Apague-o em EC2 > Load Balancers e rode o script de novo."
    exit 1
  fi
else
  echo "Cluster EKS não encontrado; pulando remoção do Ingress."
fi

# 2. Load Balancer Controller
if [[ -n "$(terraform -chdir="$TF_ADDONS" state list 2>/dev/null)" ]]; then
  echo
  echo "==> Destruindo AWS Load Balancer Controller"
  terraform -chdir="$TF_ADDONS" destroy -auto-approve \
    -var="aws_region=$REGION" \
    -var="cluster_name=$CLUSTER" \
    -var="vpc_id=$VPC_ID" \
    -var="controller_role_arn=$CONTROLLER_ROLE"
fi

# 3. Proteção contra exclusão do RDS
PROTECTED="$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].DeletionProtection' --output text 2>/dev/null || echo "None")"
if [[ "$PROTECTED" == "True" ]]; then
  echo
  echo "==> Desligando proteção contra exclusão do RDS"
  aws rds modify-db-instance --region "$REGION" --db-instance-identifier "$DB_IDENTIFIER" \
    --no-deletion-protection --apply-immediately >/dev/null
  aws rds wait db-instance-available --region "$REGION" --db-instance-identifier "$DB_IDENTIFIER"
fi

# 4. Infraestrutura principal
if [[ -n "$(terraform -chdir="$TF_MAIN" state list 2>/dev/null)" ]]; then
  echo
  echo "==> Destruindo infraestrutura principal (leva ~15-20 min)"
  terraform -chdir="$TF_MAIN" destroy -auto-approve
fi

# 5. Conferência final
echo
echo "==> Conferindo recursos remanescentes em $REGION"
LEFTOVERS=0

check() {
  local label="$1" result="$2"
  if [[ -n "$result" && "$result" != "None" ]]; then
    echo "  [!] $label: $result"
    LEFTOVERS=1
  fi
}

check "Cluster EKS" "$(aws eks list-clusters --region "$REGION" \
  --query "clusters[?@=='$CLUSTER']" --output text)"
check "Instâncias EC2" "$(aws ec2 describe-instances --region "$REGION" \
  --filters "Name=tag:eks:cluster-name,Values=$CLUSTER" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query 'Reservations[].Instances[].InstanceId' --output text)"
check "NAT Gateways" "$(aws ec2 describe-nat-gateways --region "$REGION" \
  --filter "Name=tag:Project,Values=$CLUSTER" "Name=state,Values=pending,available" \
  --query 'NatGateways[].NatGatewayId' --output text)"
check "Elastic IPs" "$(aws ec2 describe-addresses --region "$REGION" \
  --filters "Name=tag:Project,Values=$CLUSTER" \
  --query 'Addresses[].PublicIp' --output text)"
check "Load Balancers" "$(aws resourcegroupstaggingapi get-resources --region "$REGION" \
  --resource-type-filters elasticloadbalancing:loadbalancer \
  --tag-filters "Key=elbv2.k8s.aws/cluster,Values=$CLUSTER" \
  --query 'ResourceTagMappingList[].ResourceARN' --output text)"
check "Volumes EBS soltos" "$(aws ec2 describe-volumes --region "$REGION" \
  --filters "Name=tag:kubernetes.io/cluster/$CLUSTER,Values=owned" "Name=status,Values=available" \
  --query 'Volumes[].VolumeId' --output text)"
check "Instância RDS" "$(aws rds describe-db-instances --region "$REGION" \
  --query "DBInstances[?DBInstanceIdentifier=='$DB_IDENTIFIER'].DBInstanceIdentifier" --output text)"
check "Snapshots RDS" "$(aws rds describe-db-snapshots --region "$REGION" --snapshot-type manual \
  --query "DBSnapshots[?starts_with(DBSnapshotIdentifier, '${CLUSTER}-final')].DBSnapshotIdentifier" --output text)"

echo
if [[ "$LEFTOVERS" == "0" ]]; then
  echo "Nada cobrando encontrado. Infraestrutura removida."
else
  echo "Ainda há recursos acima. Apague-os pelo console da AWS para parar a cobrança."
  exit 1
fi
