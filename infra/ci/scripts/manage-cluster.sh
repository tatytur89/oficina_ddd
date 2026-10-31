#!/bin/bash
# Ciclo de vida exclusivo do runner Linux; não usa a raiz Windows nem AWS.
set -euo pipefail
umask 077

action=${1:?Use create, check or destroy}
[[ "$action" =~ ^(create|check|destroy)$ ]] || { echo 'Invalid action.' >&2; exit 1; }
: "${OFICINA_ROOT:?Absolute Terraform root is required}"
: "${OFICINA_NAME:?Cluster name is required}"
[[ "$OFICINA_NAME" =~ ^oficina-local-ci(-[a-z0-9]+)*$ && ${#OFICINA_NAME} -le 50 ]] || exit 1
[[ "$OFICINA_ROOT" == /* ]] || exit 1
root=$(cd "$OFICINA_ROOT" && pwd -P)
script_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
[[ "$root" == "$script_root" ]] || { echo 'Terraform root does not own this helper.' >&2; exit 1; }
generated="$root/.generated"
owner="$generated/owner.json"
kubeconfig="$generated/kubeconfig"
config="$generated/kind.json"
name=$OFICINA_NAME
context="kind-$name"
for tool in docker kind kubectl python3 flock; do command -v "$tool" >/dev/null; done
for path in "$generated" "$owner" "$kubeconfig" "$config"; do
    [[ ! -L "$path" ]] || { echo 'Managed paths must not be symbolic links.' >&2; exit 1; }
done

export KIND_EXPERIMENTAL_PROVIDER=docker
if [[ -n ${DOCKER_CONTEXT:-} ]]; then
    endpoint=$(docker context inspect "$DOCKER_CONTEXT" --format '{{.Endpoints.docker.Host}}')
elif [[ -n ${DOCKER_HOST:-} ]]; then
    endpoint=$DOCKER_HOST
else
    endpoint=$(docker context inspect --format '{{.Endpoints.docker.Host}}')
fi
[[ "$endpoint" == unix:///* && "$endpoint" != *$'\n'* ]] || {
    echo 'CI requires a local Docker Unix socket.' >&2; exit 1;
}
unset DOCKER_CONTEXT
export DOCKER_HOST="$endpoint"
export KUBECONFIG="$kubeconfig"
engine=$(docker info --format '{{.ID}}')
[[ -n "$engine" ]] || exit 1
exec 9>"/tmp/oficina-kind-$name.lock"
flock -n 9 || { echo 'Another process manages this cluster.' >&2; exit 1; }

scan_nodes() {
    local result
    result=$(docker ps -aq --no-trunc --filter "label=io.x-k8s.kind.cluster=$name") || return 1
    nodes=()
    if [[ -n "$result" ]]; then mapfile -t nodes <<< "$result"; fi
}

assert_owner() {
    [[ -f "$owner" ]] || { echo 'Ownership marker missing.' >&2; return 1; }
    python3 - "$owner" "$name" "$root" "$engine" "${nodes[@]}" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as source:
    owner = json.load(source)
if (owner.get('name'), owner.get('root'), owner.get('engine_id')) != tuple(sys.argv[2:5]):
    raise SystemExit('Ownership root, name or Docker Engine mismatch.')
if not set(sys.argv[5:]).issubset(set(owner.get('node_ids', []))):
    raise SystemExit('Unrecognized containers; refusing to manage this cluster.')
print(owner.get('status', 'unknown'))
PY
}

write_owner() {
    python3 - "$owner" "$name" "$root" "$engine" "$1" "${nodes[@]}" <<'PY'
import json, sys
with open(sys.argv[1], 'w', encoding='utf-8') as target:
    json.dump(dict(name=sys.argv[2], root=sys.argv[3], engine_id=sys.argv[4],
                   status=sys.argv[5], node_ids=sys.argv[6:]), target)
PY
}

check_cluster() {
    scan_nodes || return 1
    local status
    status=$(assert_owner) || return 1
    [[ "$status" == ready && ${#nodes[@]} -eq 2 ]] || return 1
    kubectl --kubeconfig "$kubeconfig" --context "$context" wait --for=condition=Ready nodes --all --timeout=180s || return 1
    kubectl --kubeconfig "$kubeconfig" --context "$context" get nodes -o json |
        python3 -c 'import json,sys; n=json.load(sys.stdin)["items"]; assert len(n)==2 and all(x["status"]["nodeInfo"]["kubeletVersion"]=="v1.34.11" for x in n)' || return 1
    local ready
    ready=$(kubectl --kubeconfig "$kubeconfig" --context "$context" get --raw=/readyz) || return 1
    [[ "$ready" == ok ]]
}

destroy_cluster() {
    scan_nodes || return 1
    if [[ ! -f "$owner" ]]; then
        [[ ${#nodes[@]} -eq 0 ]] || { echo 'Existing cluster has no ownership proof.' >&2; return 1; }
        return 0
    fi
    assert_owner >/dev/null || return 1
    kind delete cluster --name "$name" --kubeconfig "$kubeconfig" || return 1
    scan_nodes || return 1
    [[ ${#nodes[@]} -eq 0 ]] || return 1
    rm -f -- "$owner" "$kubeconfig" "$config"
}

case "$action" in
    create)
        [[ $(kind version) =~ v0\.33\.0 ]] || { echo 'kind v0.33.0 required.' >&2; exit 1; }
        [[ $(docker info --format '{{.OSType}}') == linux ]] || exit 1
        scan_nodes
        [[ ${#nodes[@]} -eq 0 && ! -e "$owner" && ! -e "$kubeconfig" ]] || {
            echo 'Cluster or managed files already exist; no adoption.' >&2; exit 1;
        }
        : "${OFICINA_CONFIG:?Cluster configuration is required}"
        mkdir -p "$generated"
        printf '%s' "$OFICINA_CONFIG" | base64 --decode > "$config"
        write_owner creating
        if ! kind create cluster --name "$name" --kubeconfig "$kubeconfig" --config "$config" --wait 180s; then
            # Falha não comprova propriedade dos containers encontrados depois dela.
            scan_nodes
            if [[ ${#nodes[@]} -eq 0 ]]; then rm -f -- "$owner" "$kubeconfig" "$config"; fi
            exit 1
        fi
        scan_nodes
        write_owner ready
        if ! check_cluster; then destroy_cluster; exit 1; fi
        ;;
    check) check_cluster ;;
    destroy) destroy_cluster ;;
esac
