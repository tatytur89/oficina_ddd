#!/bin/bash
set -u
destination="$RUNNER_TEMP/oficina-ci-evidence"
mkdir -p "$destination"
if [[ -f ${TF_VAR_kubeconfig_path:-/nonexistent} ]]; then
    k=(kubectl --kubeconfig "$TF_VAR_kubeconfig_path" --context "$TF_VAR_context" --request-timeout=15s)
    "${k[@]}" get nodes -o wide > "$destination/nodes.txt" 2>&1
    "${k[@]}" -n "$TF_VAR_namespace" get deployments,pods,services,ingress,hpa,jobs,pvc -o wide \
        > "$destination/workloads.txt" 2>&1
    "${k[@]}" -n "$TF_VAR_namespace" get events --sort-by=.lastTimestamp \
        > "$destination/events.txt" 2>&1
fi
# No state, rendered Secret, kubeconfig, environment dump or application logs are uploaded.
exit 0
