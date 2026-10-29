# Raízes Terraform

| Diretório | Ambiente | Responsabilidade |
|---|---|---|
| `infra/` | AWS | VPC, EKS, RDS e permissões AWS existentes |
| `infra/addons/` | AWS | Componentes Helm instalados no EKS |
| `infra/local/` | Local | Raiz isolada para o laboratório kind |

As raízes são independentes. O Terraform não carrega automaticamente configurações das subpastas. Execute sempre com `-chdir` explícito e não reutilize arquivos de estado ou variáveis entre ambientes.

A raiz AWS permaneceu em `infra/` para preservar os caminhos e estados existentes. Não é necessário mover nem migrar um estado AWS para preparar o laboratório local.

Em `infra/local/`, o Terraform cria um cluster kind com um control plane e um worker, usando Docker local e kubeconfig dedicado. Consulte [as instruções do laboratório](local/README.md). Essa raiz não usa providers ou recursos AWS.

Estados, variáveis reais e arquivos gerados não devem ser versionados. Não execute `apply` ou `destroy` na raiz AWS para validar a configuração local.
