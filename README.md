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
| GET | `/api/v1/ordens-servico/{id}/acompanhar` | Acompanhar progresso da OS (público, sem autenticação) |
| GET | `/api/v1/ordens-servico/metricas/tempo-medio-execucao` | Tempo médio de execução das OS finalizadas |

Exemplo de corpo (criar OS):

```json
{
  "clienteId": 1,
  "veiculoId": 1,
  "dataPrevistaEntrega": "2026-08-30T18:00:00",
  "observacoes": "Ruído no freio dianteiro",
  "servicos": [
    { "servicoId": 1, "quantidade": 1 }
  ],
  "pecas": [
    { "pecaId": 1, "quantidade": 1 }
  ]
}
```

### Máquina de estados da OS

```text
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
```

- Serviços e peças só podem ser adicionados à OS em `RECEBIDA` ou `EM_DIAGNOSTICO`.
- `enviarOrcamento` exige a OS em `EM_DIAGNOSTICO` e pelo menos um serviço vinculado.
- Ao transicionar para `EM_EXECUCAO`, o estoque de cada peça vinculada é baixado automaticamente.
- Não há caminho de cancelamento — cada status só avança para o próximo da lista.

## Coleção Postman

Uma coleção pronta para uso está disponível em [`postman/Oficina-DDD.postman_collection.json`](postman/Oficina-DDD.postman_collection.json), organizada em pastas por entidade (Autenticação, Cliente, Veículo, Serviço, Peça, Ordem de Serviço). O login salva o token automaticamente na variável de coleção para uso nas demais requisições.
