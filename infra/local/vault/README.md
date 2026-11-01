# HashiCorp Vault local

Esta raiz Terraform independente instala HashiCorp Vault no cluster kind `oficina-local`. O serviço é interno ao cluster (ClusterIP); os scripts locais acessam a API por `kubectl port-forward` ligado somente a `127.0.0.1`. O backend usa um PVC local de 1 GiB e armazenamento `file`. Esta configuração HTTP sem TLS, single-node, não selada automaticamente e sem alta disponibilidade serve somente ao laboratório local; não a use em produção.

## Provisionar

Crie primeiro o cluster e seu namespace de aplicação:

```powershell
terraform -chdir=infra/local init -input=false
terraform -chdir=infra/local apply
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local apply -f k8s/local/namespace.yaml
terraform -chdir=infra/local/vault init -input=false
terraform -chdir=infra/local/vault plan '-out=vault.tfplan'
terraform -chdir=infra/local/vault apply vault.tfplan
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n vault-system wait --for=jsonpath='{.status.phase}'=Running pod/vault-0 --timeout=300s
```

Inicialize e desbloqueie o Vault na primeira instalação:

```powershell
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n vault-system exec -it vault-0 -- vault operator init -key-shares=1 -key-threshold=1
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n vault-system exec -it vault-0 -- vault operator unseal
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n vault-system rollout status statefulset/vault --timeout=300s
```

Guarde a chave de unseal e o token inicial de forma segura, fora do repositório, de arquivos de plano e dos logs. O script `Set-LocalVaultSecrets.ps1` solicita o token sem eco e pede as credenciais do PostgreSQL e do admin; ele cria KV v2 `secret/` se ainda não existir. O usuário local do banco do projeto é `oficina_user`.

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/vault/scripts/Set-LocalVaultSecrets.ps1
```

O caminho KV é `secret/data/oficina-local`, com as chaves `database_username`, `database_password`, `admin_user` e `admin_password`. Os valores são enviados à API do Vault sem serem gravados em arquivos por esses scripts. O token de Vault pode ser fornecido por `$env:VAULT_TOKEN` (somente no processo/sessão local); caso contrário, os scripts o solicitam interativamente. Nunca coloque o token em argumentos de linha de comando.

## Uso pelos fluxos locais

Antes do primeiro apply do PostgreSQL, crie/atualize o Secret no namespace:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/vault/scripts/Sync-LocalVaultSecrets.ps1 -Namespace oficina-local -DatabaseOnly
```

Depois siga o [guia do PostgreSQL local](../database/README.md). O deploy da aplicação também lê os valores do Vault em tempo de execução e cria os Secrets Kubernetes necessários. Para Docker Compose, use o wrapper que mantém o token e as credenciais somente no processo:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Start-LocalCompose.ps1
```

O wrapper constrói a imagem sem passar segredos ao Docker build, então executa `docker compose up --build` com as credenciais apenas no ambiente de execução. A API Kubernetes usa as mesmas credenciais no runtime por Secrets sincronizados pelo script de deploy. `docker compose config` ou `docker inspect` podem mostrar variáveis de ambiente resolvidas; não compartilhe essa saída nem logs/diagnósticos contendo valores.

## Estado, persistência e remoção

O state desta raiz contém a configuração do Helm, não os valores KV. O Vault persiste os dados no PVC; destruição/substituição do cluster local ainda apaga o laboratório e o volume. O token inicial tem privilégios de root; use-o apenas para este ambiente e não o exponha em shell history, arquivos ou CI. Recriar ou reiniciar Vault com o mesmo PVC conserva os dados, mas exige uma chave de unseal válida.

O Secret do Kubernetes é uma cópia operacional dos valores do Vault para containers que consomem variáveis de ambiente; o conteúdo fica acessível a quem tem privilégios no cluster e a codificação Base64 do Secret não é criptografia. Proteja o kind e o kubeconfig local e não trate esse fluxo como equivalente a Vault Agent/CSI em produção.

```powershell
terraform -chdir=infra/local/vault plan -destroy '-out=vault-destroy.tfplan'
terraform -chdir=infra/local/vault apply vault-destroy.tfplan
```

O destroy remove o release do Helm; confirme manualmente o PVC `data-vault-0` antes de removê-lo, pois a retenção pode preservar os dados. Remova o PVC somente se quiser apagar definitivamente o Vault. Faça backup seguro da chave de unseal e dos dados antes de qualquer limpeza.
