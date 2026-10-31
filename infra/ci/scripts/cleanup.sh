#!/bin/bash
set -uo pipefail
cd "${GITHUB_WORKSPACE:?}"
failed=0
for root in infra/local/addons infra/local/database infra/ci; do
    if [[ -f "$root/terraform.tfstate" ]]; then
        terraform -chdir="$root" destroy -auto-approve -input=false || failed=1
    fi
done
# Terraform skips destroy provisioners for tainted resources; use the same guarded helper.
if [[ -n ${OFICINA_ROOT:-} && -f "$OFICINA_ROOT/.generated/owner.json" ]]; then
    bash infra/ci/scripts/manage-cluster.sh destroy || failed=1
fi
rm -f -- k8s/local/secret.env k8s/ci-generated/kustomization.yaml
printf '\nCleanup exit status: %s (0 means all requested operations succeeded).\n' "$failed" >> "$GITHUB_STEP_SUMMARY"
exit "$failed"
