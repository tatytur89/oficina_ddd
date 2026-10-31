#!/bin/bash
set -euo pipefail
umask 077
: "${GITHUB_WORKSPACE:?}" "${GITHUB_SHA:?}" "${OFICINA_ROOT:?}"
: "${TF_VAR_database_password:?}" "${CI_JWT_SECRET:?}" "${CI_ADMIN_PASSWORD:?}"
cd "$GITHUB_WORKSPACE"
export KUBECONFIG="$TF_VAR_kubeconfig_path"
image="oficina-ddd:ci-$GITHUB_SHA"
docker build --build-arg "APP_COMMIT_SHA=$GITHUB_SHA" --tag "$image" .
terraform -chdir=infra/ci init -input=false
terraform -chdir=infra/ci plan -input=false -out=cluster.tfplan
terraform -chdir=infra/ci apply -input=false cluster.tfplan
bash infra/ci/scripts/manage-cluster.sh check
k=(kubectl --kubeconfig "$TF_VAR_kubeconfig_path" --context "$TF_VAR_context")
"${k[@]}" apply -f k8s/local/namespace.yaml
for root in infra/local/database infra/local/addons; do
    terraform -chdir="$root" init -input=false
    terraform -chdir="$root" plan -input=false -out=ci.tfplan
    terraform -chdir="$root" apply -input=false ci.tfplan
done
kind load docker-image "$image" --name "$OFICINA_NAME"
# Never read developer mail.env. This deployment cannot send notifications.
python3 - <<'PY'
import os
values = {
    'SPRING_DATASOURCE_URL': 'jdbc:postgresql://postgres:5432/oficina_db',
    'SPRING_DATASOURCE_USERNAME': 'oficina_user',
    'SPRING_DATASOURCE_PASSWORD': os.environ['TF_VAR_database_password'],
    'JWT_SECRET': os.environ['CI_JWT_SECRET'],
    'ADMIN_USER': 'admin',
    'ADMIN_PASSWORD': os.environ['CI_ADMIN_PASSWORD'],
    'APP_NOTIFICATIONS_EMAIL_ENABLED': 'false',
}
with open('k8s/local/secret.env', 'w', encoding='utf-8') as target:
    target.write(''.join(k + '=' + v + '\n' for k, v in values.items()))
PY
mkdir -p k8s/ci-generated
cat > k8s/ci-generated/kustomization.yaml <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../local
images:
  - name: oficina-ddd
    newName: oficina-ddd
    newTag: ci-$GITHUB_SHA
patches:
  - target:
      kind: Deployment
      name: oficina-api
    patch: |-
      - op: add
        path: /spec/template/spec/containers/0/env
        value:
          - name: APP_NOTIFICATIONS_EMAIL_ENABLED
            value: "false"
EOF
"${k[@]}" apply -k k8s/ci-generated
"${k[@]}" -n "$TF_VAR_namespace" rollout status deployment/oficina-api --timeout=300s
"${k[@]}" wait --for=condition=Available apiservice/v1beta1.metrics.k8s.io --timeout=180s
mkdir -p "$RUNNER_TEMP/oficina-ci-evidence"
curl --fail --silent --show-error --retry 12 --retry-all-errors --retry-delay 5 \
    --header 'Host: oficina.local' http://127.0.0.1:8090/actuator/health \
    > "$RUNNER_TEMP/oficina-ci-evidence/health.json"
curl --fail --silent --show-error --retry 12 --retry-all-errors --retry-delay 5 \
    --header 'Host: oficina.local' http://127.0.0.1:8090/api/v1/info \
    > "$RUNNER_TEMP/oficina-ci-evidence/info.json"
python3 - <<'PY'
import json, os
from pathlib import Path
root = Path(os.environ['RUNNER_TEMP']) / 'oficina-ci-evidence'
assert json.loads((root / 'health.json').read_text())['status'] == 'UP'
info = json.loads((root / 'info.json').read_text())
assert info['dados']['commit'] == os.environ['GITHUB_SHA'], 'Deployed commit differs from tested commit'
PY
printf 'image=%s\ncommit=%s\n' "$image" "$GITHUB_SHA" >> "$GITHUB_OUTPUT"
printf '### Local delivery\n\n- Commit: `%s`\n- Image: `%s`\n- Ingress health: UP\n- SMTP: disabled\n- Cleanup follows this step.\n' \
    "$GITHUB_SHA" "$image" >> "$GITHUB_STEP_SUMMARY"
