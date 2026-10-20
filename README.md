# Oficina DDD

Projeto Spring Boot desenvolvido para o Tech Challenge da FIAP.

## Descrição

Sistema de gestão para uma oficina mecânica, cobrindo cadastro de clientes, veículos, serviços e peças, além do fluxo completo de ordens de serviço (criação, diagnóstico, orçamento, execução e entrega). Aplicação Java com Spring Boot, usando Maven Wrapper para facilitar a execução sem precisar instalar Maven globalmente.

## Arquitetura

O projeto segue arquitetura hexagonal (ports & adapters) com Domain-Driven Design:

- `domain` — entidades e value objects, sem dependência de frameworks
- `application` — casos de uso (services) e exceções de negócio
- `ports` — interfaces de entrada (`in`) e saída (`out`) que definem os contratos entre camadas
- `adapters/in/web` — controllers REST, DTOs e mappers
- `adapters/out/persistence` — adaptadores JPA/PostgreSQL
- `adapters/out/security` — autenticação JWT

## Por que PostgreSQL

O PostgreSQL foi escolhido por ser um banco relacional maduro, open-source, com suporte nativo e bem documentado no ecossistema Spring Data JPA. O domínio da oficina é fortemente relacional (cliente → veículo → ordem de serviço → peças/serviços, com chaves estrangeiras e integridade referencial reforçada na camada de aplicação), o que se encaixa melhor num modelo relacional do que num banco não-relacional. O PostgreSQL também tem excelente suporte a tipos numéricos precisos (`NUMERIC`), importante para valores monetários, e é o banco padrão de fato em ambientes de produção Java/Spring, o que reduz o custo de operação e manutenção.

## Pré-requisitos

Antes de rodar o projeto, certifique-se de ter instalado:

- Java 21 JDK
- Git

### Verificando a instalação do Java

No terminal, execute:

```bash
java -version
```

Você deve ver uma versão 21.x.

## Começando

Clone o repositório e entre na pasta do projeto:

```bash
git clone https://github.com/robert-portilho/oficina-ddd
git cd oficina-ddd
```

Se necessário, conceda permissão de execução ao Maven Wrapper:

```bash
chmod +x mvnw
```

## Comandos principais

### Executar a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação ficará disponível em:

```text
http://localhost:8080
```

### Executar testes

```bash
./mvnw test
```

### Verificar cobertura de testes (gate de 80%)

```bash
./mvnw verify
```

### Gerar pacote da aplicação

```bash
./mvnw package
```

### Limpar e compilar o projeto

```bash
./mvnw clean install
```

## Executar com Docker Compose

Para subir a aplicação e o banco PostgreSQL via Docker Compose:

```bash
docker compose up --build
```

A aplicação estará disponível em:

```text
http://localhost:8089
```

## Deploy no Kubernetes

Os manifests em `k8s/` publicam a API usando um PostgreSQL externo. Para deploy automatizado, cada push em `main` que conclui o CI com sucesso dispara o workflow **CD Pipeline**, que publica a imagem com a tag `sha-<commit>` no GHCR e atualiza o Deployment. Configure o secret `KUBE_CONFIG` nas configurações do repositório com o conteúdo do kubeconfig. A imagem do GHCR precisa ser pública para que o cluster possa baixá-la.

Antes do primeiro deploy automatizado, crie o namespace `oficina-api` e o Secret `oficina-api-secret` no cluster. O workflow verifica se esse Secret existe e não aplica `secret.yaml.example`, que contém apenas valores placeholder.

Para aplicar os manifests manualmente:

1. Publique a imagem pelo workflow **Build and Push Docker Image** em Actions e anote uma tag existente no GHCR (por exemplo, `sha-<sha-curto>`). Atualize `image` em `k8s/deployment.yaml` com essa tag.
2. Ajuste o host e, se necessário, `ingressClassName` em `k8s/ingress.yaml`. O cluster precisa ter um controlador Ingress instalado e DNS apontando para ele.
3. Copie `k8s/secret.yaml.example` para `k8s/secret.yaml` e substitua todos os valores `REPLACE_WITH_...` pela URL JDBC e credenciais do banco externo, segredo JWT aleatório (mínimo de 32 bytes) e credenciais do administrador. O arquivo `k8s/secret.yaml` é ignorado pelo Git; não versione segredos reais. O exemplo não deve ser aplicado sem edição.
4. Crie o namespace (uma vez) e aplique os recursos:

```bash
kubectl create namespace oficina-api
kubectl apply -n oficina-api -f k8s/
kubectl rollout status -n oficina-api deployment/oficina-api
```

No PowerShell, use `Copy-Item k8s\secret.yaml.example k8s\secret.yaml` para copiar o exemplo. Se o pacote GHCR for privado, crie um `imagePullSecret` no namespace e referencie-o em `spec.template.spec.imagePullSecrets` no Deployment. O Ingress de exemplo não configura TLS; em produção, configure `spec.tls` com um certificado apropriado.

O Deployment e o Service usam a porta 8080 (a porta interna real da aplicação). As probes usam os endpoints de liveness/readiness do Actuator, liberados sem autenticação para uso pelo Kubernetes; a readiness também considera a conexão ao banco. O `ConfigMap` guarda apenas configurações não sensíveis; credenciais ficam no `Secret`. Os valores de `resources` são uma base inicial e devem ser ajustados conforme o consumo observado no cluster.

O perfil `docker` está configurado com `spring.jpa.hibernate.ddl-auto=validate`: prepare previamente o schema do banco externo para corresponder à aplicação. Esses manifests não criam tabelas nem executam migrações.

## Infraestrutura AWS com Terraform

A configuração em `terraform/` cria uma VPC em duas zonas, EKS com nós privados, PostgreSQL RDS privado e criptografado com senha gerenciada pelo Secrets Manager, regras de rede e a role IAM do AWS Load Balancer Controller. O controller é instalado por uma segunda configuração Terraform em `terraform/addons/`, depois que o cluster já existe; isso evita inicializar o provider Kubernetes antes do EKS estar disponível.

Você precisa de Terraform 1.5+, AWS CLI, `kubectl`, Helm 3 e credenciais AWS com permissões para criar EKS, VPC, RDS, IAM e Secrets Manager. Configure o perfil AWS e copie o exemplo de variáveis no PowerShell:

```powershell
Copy-Item terraform\terraform.tfvars.example terraform\terraform.tfvars
```

Edite `terraform/terraform.tfvars`, substituindo `REPLACE_WITH_YOUR_PUBLIC_IP/32` pelo seu IP público e escolhendo região/valores apropriados. O CIDR `0.0.0.0/0` é rejeitado para proteger o endpoint público do EKS. Em seguida:

```powershell
terraform -chdir=terraform init
terraform -chdir=terraform plan
terraform -chdir=terraform apply
$region = terraform -chdir=terraform output -raw aws_region
$cluster = terraform -chdir=terraform output -raw cluster_name
aws eks update-kubeconfig --region $region --name $cluster
terraform -chdir=terraform/addons init
$vpc = terraform -chdir=terraform output -raw vpc_id
$controllerRole = terraform -chdir=terraform output -raw aws_load_balancer_controller_role_arn
terraform -chdir=terraform/addons apply `
  -var="aws_region=$region" `
  -var="cluster_name=$cluster" `
  -var="vpc_id=$vpc" `
  -var="controller_role_arn=$controllerRole"
kubectl create namespace oficina-api
Copy-Item k8s\secret.yaml.example k8s\secret.yaml
```

Abra no AWS Secrets Manager o ARN exibido por `terraform -chdir=terraform output -raw database_master_secret_arn` e copie a senha gerenciada pelo RDS para `SPRING_DATASOURCE_PASSWORD` em `k8s/secret.yaml`. Preencha também `SPRING_DATASOURCE_URL` usando o endpoint de `terraform -chdir=terraform output -raw database_endpoint`, e defina valores fortes para `JWT_SECRET`, `ADMIN_USER` e `ADMIN_PASSWORD`. Depois aplique os manifests:

```powershell
kubectl apply -n oficina-api -f k8s\
kubectl rollout status -n oficina-api deployment/oficina-api
kubectl get ingress -n oficina-api oficina-api
```

Não coloque credenciais em `terraform.tfvars`, no Git ou na linha de comando. Se a imagem GHCR for privada, crie um `imagePullSecret` no namespace e referencie-o no Deployment.

O RDS é criado sem tabelas. Como o perfil `docker` usa `spring.jpa.hibernate.ddl-auto=validate`, inicialize o schema por uma migração/script SQL aprovado antes de iniciar os pods; este repositório não inclui migrações de banco.

Por padrão, a configuração usa nós `t3.medium`, RDS `db.t4g.micro`, um NAT Gateway e banco Single-AZ; esses recursos e o endpoint EKS geram cobrança contínua na AWS. A aplicação usa a senha mestra do RDS neste exemplo; para produção, crie um usuário PostgreSQL exclusivo com privilégios mínimos e use suas credenciais no Secret. O RDS tem proteção contra exclusão habilitada; para removê-lo intencionalmente, altere `database_deletion_protection` para `false`, aplique essa mudança e só então destrua a infraestrutura. Antes de destruir o cluster, remova o Ingress/ALB e o controller (`kubectl delete ingress -n oficina-api oficina-api` e `terraform -chdir=terraform/addons destroy`, informando os mesmos valores `-var` usados ao aplicar); o snapshot final do banco permanece na AWS e pode continuar gerando custo. O ALB é HTTP; configure certificado ACM e TLS antes de expor dados reais. O estado local do Terraform deve ser protegido e migrado para backend remoto criptografado/lock antes de uso por equipe ou produção.

## Interface Swagger

A documentação interativa da API pode ser acessada em:

```text
http://localhost:8080/swagger-ui/index.html
```

## Formato padrão de resposta

A maioria dos endpoints retorna o corpo envelopado no formato:

```json
{
  "status": "success",
  "message": "Descrição do resultado",
  "dados": { }
}
```

Em erros, `status` vem como `"error"`, `dados` vem `null`, e o HTTP status reflete o tipo de erro (400 para dados inválidos, 401 para credenciais inválidas, 404 para recurso não encontrado, 409 para conflito/dependência).

## Autenticação

### Autenticar administrador

- Método: `POST`
- URL: `/api/v1/auth/login`
- Content-Type: `application/json`

Exemplo de corpo:

```json
{
  "usuario": "admin",
  "senha": "admin123"
}
```

Retorna:

- `200 OK` com o token JWT no corpo da resposta
- `400 Bad Request` quando os dados enviados são inválidos (ex: usuário em branco)
- `401 Unauthorized` quando as credenciais são inválidas

Exemplo de resposta (sucesso):

```json
{
  "status": "success",
  "message": "Autenticação realizada com sucesso",
  "dados": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

O token JWT tem validade de 2 horas.

### Usar o token JWT

Após autenticar, use o token retornado no header `Authorization` das requisições protegidas:

```text
Authorization: Bearer <token>
```

No Swagger UI, clique em `Authorize`, cole o token JWT e confirme. Em seguida, utilize o botão `Try it out` nos endpoints protegidos.

Todas as rotas administrativas exigem autenticação, exceto: login, Swagger UI e a consulta pública de acompanhamento de OS (`GET /api/v1/ordens-servico/{id}/acompanhar`).

## Endpoints de clientes

Base: `/api/v1/clientes`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/clientes` | Cadastrar cliente |
| GET | `/api/v1/clientes` | Listar todos os clientes |
| GET | `/api/v1/clientes/documento?documento={documento}` | Buscar cliente por CPF/CNPJ |
| PUT | `/api/v1/clientes/{id}` | Atualizar cliente |
| DELETE | `/api/v1/clientes/{id}` | Excluir cliente |

Exemplo de corpo (cadastrar/atualizar):

```json
{
  "nome": "João da Silva",
  "documento": "12345678909",
  "email": "joao@example.com",
  "telefone": "11999999999"
}
```

Observações:
- `documento` deve ser único no sistema (CPF de 11 ou CNPJ de 14 dígitos, com dígito verificador válido).
- Excluir um cliente é bloqueado (`409`) se ele tiver veículos ou ordens de serviço vinculados.

## Endpoints de veículos

Base: `/api/v1/veiculos`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/veiculos` | Cadastrar veículo |
| GET | `/api/v1/veiculos` | Listar todos os veículos |
| GET | `/api/v1/veiculos/cliente/{clienteId}` | Listar veículos de um cliente |
| GET | `/api/v1/veiculos/placa/{placa}` | Buscar veículo por placa |
| PUT | `/api/v1/veiculos/{id}` | Atualizar veículo |
| DELETE | `/api/v1/veiculos/{id}` | Excluir veículo |

Exemplo de corpo (cadastrar/atualizar):

```json
{
  "marca": "Toyota",
  "modelo": "Corolla",
  "ano": 2022,
  "placa": "ABC1D23",
  "clienteId": 1
}
```

Observações:
- `placa` deve ser única, nos formatos antigo (`ABC1234`) ou Mercosul (`ABC1D23`).
- `clienteId` deve apontar para um cliente existente.
- Excluir um veículo é bloqueado (`409`) se ele tiver ordens de serviço vinculadas.

## Endpoints de serviços

Base: `/api/v1/servicos`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/servicos` | Cadastrar serviço |
| GET | `/api/v1/servicos` | Listar todos os serviços |
| GET | `/api/v1/servicos/{id}` | Buscar serviço por ID |
| PUT | `/api/v1/servicos/{id}` | Atualizar serviço |
| DELETE | `/api/v1/servicos/{id}` | Excluir serviço |

Exemplo de corpo (cadastrar/atualizar):

```json
{
  "nome": "Troca de óleo",
  "descricao": "Troca de óleo do motor",
  "preco": 150.00,
  "tipo": "MANUTENCAO",
  "tempoEstimadoMinutos": 60
}
```

`tipo` aceita: `REVISAO`, `MANUTENCAO`, `TROCA_PECA`, `ALINHAMENTO`, `BALANCEAMENTO`, `MECANICA_GERAL`, `ELETRICA`, `SUSPENSAO`, `FREIOS`.

## Endpoints de peças

Base: `/api/v1/pecas`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/pecas` | Cadastrar peça |
| GET | `/api/v1/pecas` | Listar todas as peças |
| GET | `/api/v1/pecas/{id}` | Buscar peça por ID |
| GET | `/api/v1/pecas/codigo/{codigo}` | Buscar peça por código |
| PUT | `/api/v1/pecas/{id}` | Atualizar peça (não altera estoque) |
| DELETE | `/api/v1/pecas/{id}` | Excluir peça |
| PUT | `/api/v1/pecas/{id}/repor-estoque` | Repor estoque |
| GET | `/api/v1/pecas/estoque-baixo` | Listar peças com estoque abaixo do mínimo |

Exemplo de corpo (cadastrar):

```json
{
  "nome": "Filtro de óleo",
  "descricao": "Filtro para motor",
  "codigo": "FIL001",
  "preco": 45.90,
  "quantidadeEstoque": 50,
  "estoqueMinimo": 10
}
```

Exemplo de corpo (repor estoque):

```json
{
  "quantidade": 10
}
```

Observações:
- `codigo` deve ser único.
- O estoque de uma peça é baixado automaticamente quando a OS que a utiliza entra em execução — não é preciso (nem possível) baixar estoque manualmente.
- Excluir uma peça é bloqueado (`409`) se ela estiver vinculada a uma OS ainda pendente de execução.

## Endpoints de ordens de serviço

Base: `/api/v1/ordens-servico`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/ordens-servico` | Criar OS |
| GET | `/api/v1/ordens-servico` | Listar todas as OS (administrativo) |
| GET | `/api/v1/ordens-servico/{id}` | Buscar OS por ID |
| GET | `/api/v1/ordens-servico/cliente/{clienteId}` | Listar OS de um cliente |
| GET | `/api/v1/ordens-servico/status/{status}` | Listar OS por status |
| GET | `/api/v1/ordens-servico/periodo?inicio={data}&fim={data}` | Listar OS abertas num período |
| PATCH | `/api/v1/ordens-servico/{id}/orcamento` | Enviar orçamento para aprovação |
| PATCH | `/api/v1/ordens-servico/{id}/status/{status}` | Atualizar status da OS |
| POST | `/api/v1/ordens-servico/{id}/diagnostico` | Registrar o diagnóstico completo (transiciona para `EM_DIAGNOSTICO` + adiciona serviços/peças numa só chamada) |
| POST | `/api/v1/ordens-servico/{id}/servicos` | Adicionar um serviço à OS (exige `EM_DIAGNOSTICO`) |
| POST | `/api/v1/ordens-servico/{id}/pecas` | Adicionar uma peça à OS (exige `EM_DIAGNOSTICO`) |
| GET | `/api/v1/ordens-servico/metricas/tempo-medio-execucao` | Tempo médio de execução das OS finalizadas |

A resposta autenticada da OS (criar, listar, buscar por ID) inclui o campo `chaveAcesso` — é essa chave que a equipe administrativa repassa ao cliente (não há integração de e-mail/SMS no projeto) para ele acessar a página pública de acompanhamento descrita abaixo.

Exemplo de corpo (criar OS):

```json
{
  "clienteId": 1,
  "veiculoId": 1,
  "observacoes": "Ruído no freio dianteiro"
}
```

A OS nasce em `RECEBIDA` só com a observação do problema relatado — sem serviços/peças nem previsão de entrega ainda, já que essas informações só fazem sentido depois do diagnóstico (não dá pra estimar prazo antes de saber o que precisa ser feito). Depois, o diagnóstico pode ser registrado de duas formas:

```json
// POST /api/v1/ordens-servico/{id}/diagnostico
// Transiciona para EM_DIAGNOSTICO, registra os itens e a previsão de entrega numa chamada só
{
  "servicos": [{ "servicoId": 1, "quantidade": 1 }],
  "pecas": [{ "pecaId": 1, "quantidade": 1 }],
  "dataPrevistaEntrega": "2026-08-30T18:00:00"
}
```

Ou, se preferir ajustar item a item (a OS já precisa estar em `EM_DIAGNOSTICO`, via `PATCH /{id}/status/EM_DIAGNOSTICO` ou pelo endpoint acima com listas vazias):

```json
// POST /api/v1/ordens-servico/{id}/servicos
{ "servicoId": 1, "quantidade": 1 }

// POST /api/v1/ordens-servico/{id}/pecas
{ "pecaId": 1, "quantidade": 1 }
```

### Máquina de estados da OS

```text
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
```

- Serviços e peças só podem ser adicionados à OS em `EM_DIAGNOSTICO`.
- `enviarOrcamento` exige a OS em `EM_DIAGNOSTICO` e pelo menos um serviço vinculado.
- Ao transicionar para `EM_EXECUCAO`, o estoque de cada peça vinculada é baixado automaticamente.
- Não há caminho de cancelamento — cada status só avança para o próximo da lista.

## Página de acompanhamento, aprovação e avaliação (cliente)

Como o cliente não tem login no sistema, o acompanhamento da OS, a aprovação do orçamento e a avaliação do serviço acontecem por uma página HTML pública, protegida pela `chaveAcesso` gerada para aquela OS específica (não é um endpoint de API JSON):

```text
GET  http://localhost:8080/acompanhamento/{id}?chave={chaveAcesso}
```

- Mostra o status atual, datas, serviços/peças vinculados e o valor total.
- Se o status for `AGUARDANDO_APROVACAO`, a página exibe um botão **"Aprovar orçamento"**, que envia `POST /acompanhamento/{id}/aprovar` e transiciona a OS para `EM_EXECUCAO`.
- Quando `FINALIZADA`, a página avisa que o veículo está pronto para retirada.
- Quando `ENTREGUE` e ainda não avaliada, a página exibe um formulário de avaliação (nota de 1 a 5 + comentário opcional), enviado via `POST /acompanhamento/{id}/avaliar`. Depois de avaliada, a página passa a mostrar a nota e o comentário registrados.
- A `chaveAcesso` **não expira** — permanece válida durante toda a vida da OS, inclusive depois da entrega, já que também é usada para a avaliação.
- Chave incorreta ou OS inexistente resultam numa página de erro (`400`/`404`), sem revelar dados da OS de outros clientes.

## Coleção Postman

Uma coleção pronta para uso está disponível em [`postman/Oficina-DDD.postman_collection.json`](postman/Oficina-DDD.postman_collection.json), organizada em pastas por entidade (Autenticação, Cliente, Veículo, Serviço, Peça, Ordem de Serviço). O login salva o token automaticamente na variável de coleção para uso nas demais requisições.

## Decisões arquiteturais

O documento [`docs/DECISOES_ARQUITETURAIS.md`](docs/DECISOES_ARQUITETURAIS.md) justifica, no formato de ADR (contexto, decisão, justificativa e alternativas consideradas), cada escolha estrutural relevante do projeto: arquitetura hexagonal, escolha do PostgreSQL, autenticação JWT, máquina de estados da OS, snapshots desnormalizados, tratamento de erro centralizado, DTOs como records, integridade referencial na camada de aplicação, ponto de baixa de estoque e estratégia de testes por camada.
