# Configurações Kubernetes por ambiente

| Diretório | Conteúdo | Aplicação direta |
|---|---|---|
| `base/` | Deployment, Service e configurações comuns | Não; a imagem é um marcador sem ambiente |
| `local/` | Namespace oficina-local, imagem local, perfil lab e Secret de desenvolvimento | Somente quando cluster, imagem e banco locais estiverem preparados |
| `aws/` | Perfil aws, imagem GHCR, Ingress ALB e HPA existentes | Somente no contexto EKS configurado |

Escolha explicitamente a configuração. Não aplique a pasta `k8s/` recursivamente nem a base isolada.

## Renderizar o ambiente local sem acessar cluster

```powershell
Copy-Item k8s/local/secret.env.example k8s/local/secret.env
kubectl kustomize k8s/local
```

O arquivo `secret.env` é ignorado pelo Git. Os valores do exemplo servem apenas para desenvolvimento. A saída contém o Secret codificado e não deve ser enviada ao repositório ou a logs públicos. Sem esse arquivo, a renderização local falha em vez de herdar segredos AWS.

O ConfigMap e o Secret locais recebem nomes com hash, e o Kustomize atualiza as referências do Deployment. A imagem padrão `oficina-ddd:local` é substituída por uma tag exclusiva pelo [script de deploy](local/README.md), que constrói e carrega o artefato nos nós com `imagePullPolicy: Never`. O Service escuta na porta 80 e encaminha para 8080 no container. O banco é provisionado por [Terraform separado](../infra/local/database/README.md), com Service `postgres` no mesmo namespace e Job de migração. Mantenha a senha de `secret.env` igual à variável do banco. Não aplique o Deployment apenas para preparar o PostgreSQL. Ingress e HPA locais serão preparados nas etapas seguintes.

## Renderizar o ambiente AWS

```powershell
kubectl kustomize k8s/aws
```

O namespace é `oficina-api`. O Secret `oficina-api-secret` é um pré-requisito externo e não é gerado com valores de desenvolvimento. Use `aws/secret.yaml.example` como referência e mantenha o arquivo real `aws/secret.yaml` fora do Git.

O CD aplica esta configuração com patch da imagem identificado pelo SHA testado. O Ingress ALB e o HPA de 2 a 5 réplicas pertencem apenas ao ambiente AWS. Os comandos de aplicação precisam indicar `--context` e namespace do EKS; renderizar com `kubectl kustomize` não realiza deploy.
