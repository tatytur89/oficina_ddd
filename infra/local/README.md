# Cluster Kubernetes local

Esta raiz Terraform cria um cluster kind com um control plane e um worker no Docker local. Usa Kubernetes **1.34.11**, com imagem fixada por digest, API em `127.0.0.1` e kubeconfig exclusivo em `.generated/kubeconfig`. Não requer conta AWS.

## Criar e verificar

Requisitos: Windows com PowerShell 5.1+, Docker em modo Linux, Terraform 1.5+, kind **0.33.0** e kubectl compatível com Kubernetes 1.34 no PATH. Execute na raiz do repositório:

```powershell
terraform -chdir=infra/local init -input=false
terraform -chdir=infra/local plan '-out=local.tfplan'
terraform -chdir=infra/local apply local.tfplan
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/scripts/check.ps1
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local get nodes
```

O nome padrão é `oficina-local`. Se ele já existir, a criação falha sem adotar ou excluir o cluster. O kubeconfig padrão e o contexto atual do usuário são preservados: sempre informe os parâmetros explícitos acima. Não publique o conteúdo do kubeconfig; ele contém credenciais administrativas.

Os parâmetros opcionais são `cluster_name`, `http_port` e `https_port`. Para personalizar, crie `infra/local/terraform.tfvars`, ignorado pelo Git:

```hcl
cluster_name = "oficina-local"
http_port    = 8090
https_port   = 8443
```

Nomes alternativos devem começar por `oficina-local-`. Ao verificar um nome alternativo, passe `-Name <nome>` ao script `check.ps1` e use o contexto `kind-<nome>` no kubectl. Portas devem estar livres e ser distintas. HTTP/HTTPS ficam vinculados somente a `127.0.0.1`; esses endereços são reservas para o futuro Ingress, ainda sem aplicação ou TLS nesta etapa.

## Estado e recuperação

O recurso nativo `terraform_data` executa a CLI kind por scripts PowerShell. Alterações da configuração ou dos scripts de criação, remoção e funções comuns provocam substituição do cluster; alterações somente no verificador não provocam substituição. **Substituir ou destruir remove os dados que estiverem dentro dele.**

O Terraform guarda o ciclo de vida, mas não consulta o Docker a cada refresh. Um plano sem alterações, sozinho, não comprova que o cluster ainda existe ou está saudável. Execute `check.ps1` após cada apply e antes de reutilizar o ambiente. Ele confere os IDs dos containers registrados, dois nós Ready, versão e prontidão da API.

Se o cluster tiver sido removido fora do Terraform, confirme a ausência antes de recriá-lo explicitamente:

```powershell
kind get clusters
terraform -chdir=infra/local plan '-replace=terraform_data.cluster' '-out=local.tfplan'
terraform -chdir=infra/local apply local.tfplan
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/scripts/check.ps1
```

O arquivo `.generated/owner.json` registra a raiz, nome, identidade do Docker Engine e IDs dos containers criados. A remoção recusa containers desconhecidos ou outro Engine. Somente Docker via named pipe ou socket Unix local é aceito; kind é executado com provider Docker. Operações de criação/remoção do mesmo nome são serializadas entre cópias do projeto na mesma sessão Windows. O mecanismo não impede comandos manuais externos; uma criação que falha nunca adota containers encontrados para apagá-los.

Preserve o registro de propriedade, o kubeconfig e `terraform.tfstate` até concluir a remoção. Não copie ou mova a raiz com seu estado para outro caminho enquanto o cluster existir; os caminhos de propriedade não são migrados automaticamente. Se faltarem os arquivos de propriedade e o cluster ainda existir, investigue a origem antes de qualquer exclusão; não importe nem adote automaticamente. Na falha de criação, kind tenta limpar seus próprios nós; se restarem containers sem propriedade comprovada, os arquivos de diagnóstico são preservados para investigação.

## Remover

```powershell
terraform -chdir=infra/local plan -destroy '-out=destroy.tfplan'
terraform -chdir=infra/local apply destroy.tfplan
```

Isso remove somente o cluster registrado nesta raiz. A imagem de nós fica no cache Docker. Estado, planos, variáveis reais e `.generated/` são ignorados pelo Git. Não execute esses comandos na raiz AWS (`infra/`).

## Teste completo do ciclo de vida

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalCluster.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalClusterGuards.ps1
```

O teste copia a configuração para um diretório temporário, usa nome e portas próprios, cria o cluster, verifica os dois nós e a API, confere idempotência e destrói o cluster de teste. Compara os containers alheios, configurações Docker/Kubernetes e PATH antes/depois. Na falha de remoção, preserva o diretório temporário e informa seu caminho para recuperação.

O segundo comando usa ferramentas simuladas em processos isolados para verificar concorrência, troca de Engine, endpoints remotos, colisão de nomes e falhas parciais. Ele não executa Docker, kind ou kubectl reais.

O próximo passo é [provisionar PostgreSQL e executar as migrações](database/README.md), em raiz Terraform independente. Aplicação, Ingress e HPA serão preparados posteriormente. A execução local não homologa o provisionamento EKS/RDS.
