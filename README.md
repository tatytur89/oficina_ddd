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

## Ambientes de execução

O código da aplicação é compartilhado. As configurações de desenvolvimento, laboratório local e AWS são selecionadas explicitamente:

| Ambiente | Perfil Spring | Configuração da infraestrutura |
|---|---|---|
| Maven na máquina | `local` (padrão) | PostgreSQL em localhost |
| Docker Compose | `docker`, com configurações locais | `docker-compose.yaml` |
| Kubernetes local | `lab` | `k8s/local/` e raiz Terraform `infra/local/` |
| AWS/EKS | `aws` | `k8s/aws/`, Terraform `infra/` e `infra/addons/` |

`k8s/base/` contém apenas os recursos comuns. Escolha `k8s/local` ou `k8s/aws` ao renderizar/aplicar; não aplique a pasta raiz recursivamente. Os perfis `lab` e `aws` exigem credenciais externas e validam o schema. Os valores padrão de desenvolvimento ficam restritos aos perfis locais.

A separação local está preparada para as próximas etapas: ainda é necessário implementar o provisionamento do cluster, banco e deploy locais. Renderizar os manifests não comprova que a infraestrutura está em execução. Consulte [as configurações Kubernetes](k8s/README.md) e [as raízes Terraform](infra/README.md).

## Por que PostgreSQL

O PostgreSQL foi escolhido por ser um banco relacional maduro, open-source, com suporte nativo e bem documentado no ecossistema Spring Data JPA. O domínio da oficina é fortemente relacional (cliente → veículo → ordem de serviço → peças/serviços, com chaves estrangeiras e integridade referencial reforçada na camada de aplicação), o que se encaixa melhor num modelo relacional do que num banco não-relacional. O PostgreSQL também tem excelente suporte a tipos numéricos precisos (`NUMERIC`), importante para valores monetários, e é o banco padrão de fato em ambientes de produção Java/Spring, o que reduz o custo de operação e manutenção.

## Executar localmente

### Pré-requisitos

- Git
- Docker com o plugin Compose (`docker compose version`)
- Java 21 JDK (`java -version` deve mostrar 21.x) — necessário apenas para rodar com Maven ou executar os testes

No Linux, se o Docker retornar `permission denied ... docker.sock`, adicione seu usuário ao grupo `docker` e abra uma nova sessão (ou rode `newgrp docker`):

```bash
sudo usermod -aG docker $USER
```

### Clonar o repositório

```bash
git clone https://github.com/tatytur89/oficina_ddd.git
cd oficina_ddd
chmod +x mvnw
```

### Pré-requisitos para Kubernetes local

Para o laboratório Kubernetes local, disponibilize as ferramentas abaixo. As versões indicadas foram verificadas na preparação do ambiente em 29/10/2026:

| Ferramenta | Versão verificada | Finalidade |
|---|---|---|
| Docker Engine | 29.4.2 | Executar containers Linux |
| Docker Compose | 5.1.3 | Ambiente de desenvolvimento |
| Terraform | 1.17.1 | Provisionamento como código |
| kind | 0.33.0 | Cluster Kubernetes local em containers |
| kubectl | 1.34.1 | Operação e inspeção do cluster |
| Helm | 3.22.1 | Instalação de componentes do cluster |

Instale os binários pelas fontes oficiais e confira os checksums publicados: [Terraform](https://developer.hashicorp.com/terraform/install), [kind](https://kind.sigs.k8s.io/docs/user/quick-start/) e [Helm](https://helm.sh/docs/intro/install/). Mantenha os executáveis no PATH e abra um novo terminal após a instalação. No Windows, use o Docker Desktop com backend WSL 2 e containers Linux.

Verifique as ferramentas e a comunicação com o Docker:

```bash
docker version
docker compose version
terraform version
kind version
kubectl version --client
helm version --short
docker run --rm hello-world
```

O teste `hello-world` deve terminar com sucesso e remover seu próprio container. A imagem permanece no cache. Como referência inicial para o laboratório, reserve 4 CPUs, 8 GiB de memória para o Docker e 20 GiB livres para imagens e dados; a carga real será medida durante o deploy e os testes de escala.

Essa verificação prepara as ferramentas, mas não cria o cluster, não provisiona recursos AWS e não valida o deploy. A imagem Kubernetes escolhida para o kind deverá ser compatível com a versão do kubectl. A configuração Terraform e os manifests AWS existentes são descritos nas seções próprias abaixo.

### Banco de dados

O perfil `local` usa `spring.jpa.hibernate.ddl-auto=update`: ao subir, o Hibernate cria/atualiza as tabelas em um banco vazio. O perfil `docker` mantém esse comportamento para desenvolvimento com Compose. Os perfis `lab` e `aws` usam `validate` e exigem preparação prévia do schema (veja [Deploy no Kubernetes](#deploy-no-kubernetes)).

Ao iniciar, a aplicação cria o usuário administrador padrão `admin` / `admin123` (configurável pelas variáveis `ADMIN_USER` e `ADMIN_PASSWORD`).

### Opção 1: Docker Compose (aplicação + PostgreSQL)

```bash
docker compose up --build
```

- API: `http://localhost:8089`
- Swagger: `http://localhost:8089/swagger-ui/index.html`
- PostgreSQL: `localhost:5432` (banco `oficina_db`, usuário `oficina_user`, senha `password123`)

Para parar (adicione `-v` para apagar também os dados do banco):

```bash
docker compose down
```

### Opção 2: Maven (aplicação na máquina, PostgreSQL no Docker)

Suba apenas o banco e depois a aplicação:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

A aplicação espera o PostgreSQL em `localhost:5432` com as credenciais acima (veja `src/main/resources/application.properties`).

### Porta 5432 ocupada

Se já houver outro PostgreSQL usando a porta 5432, o Compose falha ao publicar o banco (ou a aplicação via Maven conecta no banco errado e retorna `password authentication failed for user "oficina_user"`). Pare o outro PostgreSQL ou altere a porta publicada em `docker-compose.yaml` para `"5433:5432"`; nesse caso, ao rodar via Maven, informe a nova porta:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/oficina_db ./mvnw spring-boot:run
```

### Testar a API

1. Autentique com `POST /api/v1/auth/login` usando `admin` / `admin123` (veja [Autenticação](#autenticação)).
2. Use o token no Swagger (`Authorize`) ou importe a coleção `postman/Oficina-DDD.postman_collection.json` no Postman ou no Bruno (`Import Collection → Postman Collection`). A coleção aponta para `http://localhost:8089` (Docker Compose); ao rodar via Maven, troque para a porta `8080`.

## Comandos Maven

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

## Configuração do Kubernetes local

A configuração local usa o namespace `oficina-local`, a imagem `oficina-ddd:local` e o perfil `lab`. Ela não referencia EKS, RDS, ALB ou credenciais AWS. Prepare o arquivo local de desenvolvimento e confira a renderização:

```powershell
Copy-Item k8s/local/secret.env.example k8s/local/secret.env
kubectl kustomize k8s/local
```

`secret.env` é ignorado pelo Git. A saída renderizada contém os valores do Secret codificados; não a publique. Esse comando não acessa o cluster e não realiza deploy. Banco, imagem carregada no kind e componentes de entrada/escala serão preparados nas etapas seguintes.

## Deploy no Kubernetes

Os manifests em `k8s/aws/` publicam a API no EKS usando um PostgreSQL externo e o perfil `aws`. Para deploy automatizado, cada push em `main` que conclui o CI com sucesso dispara o workflow **CD Pipeline**, que publica a imagem com a tag `sha-<commit>` no GHCR e aplica a configuração AWS com essa imagem. Configure o secret `KUBE_CONFIG` nas configurações do repositório com o conteúdo do kubeconfig. A imagem do GHCR precisa ser pública para que o cluster possa baixá-la.

Antes do primeiro deploy automatizado, crie o namespace `oficina-api` e o Secret `oficina-api-secret` no cluster. O workflow verifica se esse Secret existe e não aplica `secret.yaml.example`, que contém apenas valores placeholder.

Para aplicar os manifests manualmente:

1. Publique a imagem pelo workflow **Build and Push Docker Image** em Actions e anote uma tag existente no GHCR (por exemplo, `sha-<sha-curto>`). Atualize `images.newTag` em `k8s/aws/kustomization.yaml` com essa tag.
2. Ajuste o host e, se necessário, `ingressClassName` em `k8s/aws/ingress.yaml`. O cluster precisa ter um controlador Ingress instalado e DNS apontando para ele.
3. Copie `k8s/aws/secret.yaml.example` para `k8s/aws/secret.yaml` e substitua todos os valores `REPLACE_WITH_...` pela URL JDBC e credenciais do banco externo, segredo JWT aleatório (mínimo de 32 bytes) e credenciais do administrador. O arquivo real é ignorado pelo Git; não versione segredos. O exemplo não deve ser aplicado sem edição.
4. Crie o namespace (uma vez) e aplique os recursos:

```powershell
$eksContext = 'SEU_CONTEXTO_EKS'
kubectl --context $eksContext create namespace oficina-api
kubectl --context $eksContext apply -n oficina-api -f k8s/aws/secret.yaml
kubectl --context $eksContext apply -n oficina-api -k k8s/aws
kubectl --context $eksContext rollout status -n oficina-api deployment/oficina-api
```

No PowerShell, use `Copy-Item k8s/aws/secret.yaml.example k8s/aws/secret.yaml` para copiar o exemplo. Se o pacote GHCR for privado, crie um `imagePullSecret` no namespace e adicione a referência ao Deployment pela configuração AWS. O Ingress de exemplo não configura TLS; em produção, configure um certificado apropriado.

O Service expõe a porta 80 e encaminha para a porta 8080 do container no Deployment. As probes usam os endpoints de liveness/readiness do Actuator, liberados sem autenticação para uso pelo Kubernetes; a readiness também considera a conexão ao banco. O `ConfigMap` guarda apenas configurações não sensíveis; credenciais ficam no `Secret`. Os valores de `resources` são uma base inicial e devem ser ajustados conforme o consumo observado no cluster.

O perfil `aws` está configurado com `spring.jpa.hibernate.ddl-auto=validate`: prepare previamente o schema do banco externo para corresponder à aplicação. Esses manifests não criam tabelas nem executam migrações.

## Infraestrutura AWS com Terraform

A configuração em `infra/` cria uma VPC em duas zonas, EKS com nós privados, PostgreSQL RDS privado e criptografado com senha gerenciada pelo Secrets Manager, regras de rede e a role IAM do AWS Load Balancer Controller. O controller é instalado por uma segunda configuração Terraform em `infra/addons/`, depois que o cluster já existe; isso evita inicializar o provider Kubernetes antes do EKS estar disponível.

Você precisa de Terraform 1.5+, AWS CLI, `kubectl`, Helm 3 e credenciais AWS com permissões para criar EKS, VPC, RDS, IAM e Secrets Manager. Configure o perfil AWS e copie o exemplo de variáveis no PowerShell:

```powershell
Copy-Item infra\terraform.tfvars.example infra\terraform.tfvars
```

Edite `infra/terraform.tfvars`, substituindo `REPLACE_WITH_YOUR_PUBLIC_IP/32` pelo seu IP público e escolhendo região/valores apropriados. O CIDR `0.0.0.0/0` é rejeitado para proteger o endpoint público do EKS. Em seguida:

```powershell
terraform -chdir=infra init
terraform -chdir=infra plan
terraform -chdir=infra apply
$region = terraform -chdir=infra output -raw aws_region
$cluster = terraform -chdir=infra output -raw cluster_name
aws eks update-kubeconfig --region $region --name $cluster
terraform -chdir=infra/addons init
$vpc = terraform -chdir=infra output -raw vpc_id
$controllerRole = terraform -chdir=infra output -raw aws_load_balancer_controller_role_arn
terraform -chdir=infra/addons apply `
  -var="aws_region=$region" `
  -var="cluster_name=$cluster" `
  -var="vpc_id=$vpc" `
  -var="controller_role_arn=$controllerRole"
$eksContext = 'SEU_CONTEXTO_EKS'
kubectl --context $eksContext create namespace oficina-api
Copy-Item k8s/aws/secret.yaml.example k8s/aws/secret.yaml
```

Abra no AWS Secrets Manager o ARN exibido por `terraform -chdir=infra output -raw database_master_secret_arn` e copie a senha gerenciada pelo RDS para `SPRING_DATASOURCE_PASSWORD` em `k8s/aws/secret.yaml`. Preencha também `SPRING_DATASOURCE_URL` usando o endpoint de `terraform -chdir=infra output -raw database_endpoint`, e defina valores fortes para `JWT_SECRET`, `ADMIN_USER` e `ADMIN_PASSWORD`. Depois aplique os manifests no contexto EKS escolhido:

```powershell
kubectl --context $eksContext apply -n oficina-api -f k8s/aws/secret.yaml
kubectl --context $eksContext apply -n oficina-api -k k8s/aws
kubectl --context $eksContext rollout status -n oficina-api deployment/oficina-api
kubectl --context $eksContext get ingress -n oficina-api oficina-api
```

Não coloque credenciais em `terraform.tfvars`, no Git ou na linha de comando. Se a imagem GHCR for privada, crie um `imagePullSecret` no namespace e referencie-o no Deployment.

O RDS é criado sem tabelas. Como o perfil `aws` usa `spring.jpa.hibernate.ddl-auto=validate`, inicialize o schema por uma migração/script SQL aprovado antes de iniciar os pods. O SQL incremental existente não cria o schema inicial; migrações completas ainda serão implementadas.

Por padrão, a configuração usa nós `t3.medium`, RDS `db.t4g.micro`, um NAT Gateway e banco Single-AZ; esses recursos e o endpoint EKS geram cobrança contínua na AWS. A aplicação usa a senha mestra do RDS neste exemplo; para produção, crie um usuário PostgreSQL exclusivo com privilégios mínimos e use suas credenciais no Secret. O RDS tem proteção contra exclusão habilitada; para removê-lo intencionalmente, altere `database_deletion_protection` para `false`, aplique essa mudança e só então destrua a infraestrutura. Antes de destruir o cluster, remova o Ingress/ALB e o controller (`kubectl --context $eksContext delete ingress -n oficina-api oficina-api` e `terraform -chdir=infra/addons destroy`, informando os mesmos valores `-var` usados ao aplicar); o snapshot final do banco permanece na AWS e pode continuar gerando custo. A escalabilidade horizontal é feita pelo `k8s/aws/hpa.yaml`: o HPA mantém de 2 a 5 réplicas conforme a CPU (alvo de 70% do `request`), usando o metrics-server instalado por `infra/addons/`. O Deployment não fixa `replicas`, para não conflitar com o HPA. Para testes curtos, use o perfil `infra/estudante.tfvars.example` (mantém os nós padrão; apenas desliga a proteção contra exclusão e o snapshot final do RDS) e, ao terminar, rode `./scripts/destroy.sh` (bash): ele remove o Ingress e espera o ALB sumir, destrói o controller e a infraestrutura principal e lista qualquer recurso que tenha ficado cobrando. O ALB é HTTP; configure certificado ACM e TLS antes de expor dados reais. O estado local do Terraform deve ser protegido e migrado para backend remoto criptografado/lock antes de uso por equipe ou produção.

## Interface Swagger

A documentação interativa da API pode ser acessada em:

```text
http://localhost:8080/swagger-ui/index.html   (Maven)
http://localhost:8089/swagger-ui/index.html   (Docker Compose)
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

As rotas administrativas exigem JWT. Login, Swagger, healthcheck, informações de versão e a página de acompanhamento são públicos. A página valida a chave própria da OS; o callback `POST /api/v1/ordens-servico/{id}/decisao-orcamento` não exige JWT e autentica a decisão pela `chave` da OS enviada no corpo, junto com a versão do orçamento.

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
| GET | `/api/v1/ordens-servico` | Listar fila operacional de OS por prioridade e antiguidade |
| GET | `/api/v1/ordens-servico/{id}` | Buscar OS por ID |
| GET | `/api/v1/ordens-servico/cliente/{clienteId}` | Listar OS de um cliente |
| GET | `/api/v1/ordens-servico/status/{status}` | Listar OS por status |
| GET | `/api/v1/ordens-servico/periodo?inicio={data}&fim={data}` | Listar OS abertas num período |
| PATCH | `/api/v1/ordens-servico/{id}/orcamento` | Enviar orçamento para aprovação |
| POST | `/api/v1/ordens-servico/{id}/decisao-orcamento` | Receber aprovação ou recusa externa por chave e versão do orçamento, sem JWT |
| PATCH | `/api/v1/ordens-servico/{id}/status/{status}` | Atualizar status da OS |
| POST | `/api/v1/ordens-servico/{id}/diagnostico` | Registrar o diagnóstico completo (transiciona para `EM_DIAGNOSTICO` + adiciona serviços/peças numa só chamada) |
| POST | `/api/v1/ordens-servico/{id}/servicos` | Adicionar um serviço à OS (exige `EM_DIAGNOSTICO`) |
| POST | `/api/v1/ordens-servico/{id}/pecas` | Adicionar uma peça à OS (exige `EM_DIAGNOSTICO`) |
| GET | `/api/v1/ordens-servico/metricas/tempo-medio-execucao` | Tempo médio de execução das OS finalizadas |

### Fila operacional e histórico

`GET /api/v1/ordens-servico` retorna apenas as OS em atendimento, nesta ordem de prioridade:

1. `EM_EXECUCAO`
2. `AGUARDANDO_APROVACAO`
3. `EM_DIAGNOSTICO`
4. `RECEBIDA`

Dentro de cada status, as OS são ordenadas por data de abertura crescente (mais antigas primeiro), com ID crescente para desempatar datas iguais. `FINALIZADA` e `ENTREGUE` ficam fora dessa fila por filtro de consulta; nenhum registro é apagado ou alterado por essa listagem.

O histórico continua disponível nas consultas por ID, cliente, status e período. Por exemplo, `GET /api/v1/ordens-servico/status/FINALIZADA` permite consultar OS finalizadas. A métrica de tempo médio também continua considerando as OS concluídas, independentemente do filtro da fila.

A resposta autenticada da OS (criar, listar, buscar por ID) inclui o campo `chaveAcesso` — é essa chave que a equipe administrativa repassa ao cliente (não há integração de e-mail/SMS no projeto) para ele acessar a página pública de acompanhamento descrita abaixo.

Exemplo de corpo (criar OS):

```json
{
  "clienteId": 1,
  "veiculoId": 1,
  "observacoes": "Ruído no freio dianteiro",
  "servicos": [{ "servicoId": 1, "quantidade": 1 }],
  "pecas": [{ "pecaId": 1, "quantidade": 1 }]
}
```

A abertura recebe IDs de cliente e veículo já cadastrados e pode incluir serviços e peças solicitados, com IDs de catálogo e quantidades positivas. As listas `servicos` e `pecas` são opcionais: podem ser omitidas ou enviadas vazias para manter o fluxo de abertura sem itens.

A OS nasce em `RECEBIDA`, com identificação única e chave de acesso. Os itens informados são registrados com nome e preço do catálogo no momento da abertura, e os totais de serviços, peças e da OS são calculados automaticamente. A abertura não define previsão de entrega, não aprova o orçamento e não baixa estoque.

O diagnóstico continua sendo uma etapa posterior. Ele preserva os itens da abertura e **acrescenta** os itens enviados, sem substituir nem deduplicar automaticamente os anteriores. Não reenvie os mesmos itens apenas para confirmá-los. Para iniciar o diagnóstico e definir a previsão sem acrescentar itens:

```json
// POST /api/v1/ordens-servico/{id}/diagnostico
// Transiciona para EM_DIAGNOSTICO e define a previsão, preservando os itens da abertura
{
  "servicos": [],
  "pecas": [],
  "dataPrevistaEntrega": "2026-08-30T18:00:00"
}
```

Se o diagnóstico identificar itens adicionais, inclua-os nas listas dessa chamada ou adicione-os individualmente pelos endpoints abaixo. A adição individual exige `EM_DIAGNOSTICO`, alcançado por `PATCH /{id}/status/EM_DIAGNOSTICO` ou pelo endpoint de diagnóstico:

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

- A abertura pode registrar serviços e peças solicitados enquanto cria a OS em `RECEBIDA`. Depois de criada, os endpoints de adição de itens exigem `EM_DIAGNOSTICO`.
- `enviarOrcamento` exige a OS em `EM_DIAGNOSTICO` e pelo menos um serviço vinculado. Cada envio incrementa `versaoOrcamento` e limpa a decisão anterior.
- A aprovação da versão vigente inicia `EM_EXECUCAO` e baixa o estoque das peças. O PATCH genérico de status não pode iniciar execução. A transição para `AGUARDANDO_APROVACAO`, mesmo pelo PATCH, aplica as validações e o versionamento do envio de orçamento.
- A recusa retorna de `AGUARDANDO_APROVACAO` para `EM_DIAGNOSTICO`, preserva os itens e não baixa estoque. A equipe pode complementar o diagnóstico e enviar uma nova versão do orçamento.

### Notificação externa de aprovação ou recusa

Após `PATCH /api/v1/ordens-servico/{id}/orcamento`, use a `versaoOrcamento` retornada na resposta. Uma integração externa pode registrar a decisão sem login administrativo, apresentando a chave UUID daquela OS:

```http
POST /api/v1/ordens-servico/1/decisao-orcamento
Content-Type: application/json
```

```json
{
  "chave": "UUID-DA-OS",
  "versaoOrcamento": 1,
  "aprovado": true
}
```

Para recusar, envie `aprovado: false`. Os três campos são obrigatórios. A resposta mantém o envelope `status`, `message` e `dados`; a OS em `dados` inclui `versaoOrcamento` e `decisaoOrcamento` (`true` para aprovação, `false` para recusa e `null` enquanto aguarda decisão).

- A primeira decisão válida recebe `200`. Aprovação inicia a execução; recusa devolve a OS ao diagnóstico.
- Repetir a mesma decisão para a mesma versão é idempotente: retorna a OS atual com `200`, sem nova gravação nem baixa de estoque.
- Versão diferente da vigente, decisão contrária à já registrada, chave inválida ou estado incompatível resultam em `400`. OS inexistente resulta em `404`.
- Depois de um novo envio do orçamento, a versão anterior não pode decidir o orçamento novo. A integração deve usar a versão retornada no novo envio.
- A decisão usa bloqueio pessimista por OS dentro da transação, serializando callbacks concorrentes da mesma OS. Isso não substitui o controle de concorrência de estoque entre OS diferentes.

Na coleção Postman, **Enviar orçamento** salva `versaoOrcamento` automaticamente. As requisições JSON de aprovação e recusa são alternativas: não execute ambas para a mesma versão esperando sucesso.

### Atualização do schema para a decisão de orçamento

Antes de implantar esta versão sobre um PostgreSQL existente com `ddl-auto=validate`, aplique [`docs/sql/orcamento-decisao.sql`](docs/sql/orcamento-decisao.sql). O script acrescenta `versao_orcamento` e `decisao_orcamento` e pode ser reaplicado. Registros legados sem versão são tratados como versão zero pela leitura da aplicação; a integração deve sempre usar a versão retornada para a OS.

Esse script é uma atualização incremental: não cria o schema inicial nem automatiza o deploy do banco. O provisionamento e a execução controlada das atualizações de schema continuam sendo pré-requisitos do ambiente.

## Página de acompanhamento, aprovação e avaliação (cliente)

Como o cliente não tem login no sistema, o acompanhamento da OS, a aprovação ou recusa do orçamento e a avaliação do serviço também estão disponíveis por uma página HTML pública, protegida pela `chaveAcesso` gerada para aquela OS específica. Essa interface HTML complementa o callback JSON descrito acima:

```text
GET  http://localhost:8080/acompanhamento/{id}?chave={chaveAcesso}
```

- Mostra o status atual, datas, serviços/peças vinculados e o valor total.
- Se o status for `AGUARDANDO_APROVACAO`, a página oferece **Aprovar orçamento** e **Recusar orçamento**. Os formulários enviam `chave` e `versaoOrcamento` em campos ocultos para `POST /acompanhamento/{id}/aprovar` ou `POST /acompanhamento/{id}/recusar`. Ambos usam as mesmas regras de versão e idempotência do callback; uma página antiga não aprova uma versão nova.
- Quando `FINALIZADA`, a página avisa que o veículo está pronto para retirada.
- Quando `ENTREGUE` e ainda não avaliada, a página exibe um formulário de avaliação (nota de 1 a 5 + comentário opcional), enviado via `POST /acompanhamento/{id}/avaliar`. Depois de avaliada, a página passa a mostrar a nota e o comentário registrados.
- A `chaveAcesso` **não expira** — permanece válida durante toda a vida da OS, inclusive depois da entrega, já que também é usada para a avaliação.
- Chave incorreta ou OS inexistente resultam numa página de erro (`400`/`404`), sem revelar dados da OS de outros clientes.

## Coleção Postman

Uma coleção pronta para uso está disponível em [`postman/Oficina-DDD.postman_collection.json`](postman/Oficina-DDD.postman_collection.json), organizada em pastas por entidade (Autenticação, Cliente, Veículo, Serviço, Peça, Ordem de Serviço). O login salva o token automaticamente na variável de coleção para uso nas demais requisições.

## Decisões arquiteturais

O documento [`docs/DECISOES_ARQUITETURAIS.md`](docs/DECISOES_ARQUITETURAIS.md) justifica, no formato de ADR (contexto, decisão, justificativa e alternativas consideradas), cada escolha estrutural relevante do projeto: arquitetura hexagonal, escolha do PostgreSQL, autenticação JWT, máquina de estados da OS, snapshots desnormalizados, tratamento de erro centralizado, DTOs como records, integridade referencial na camada de aplicação, ponto de baixa de estoque e estratégia de testes por camada.
