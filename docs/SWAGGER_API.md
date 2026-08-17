# Oficina Mecânica API - Documentação

## Visão Geral

API RESTful para o Sistema Integrado de Atendimento e Execução de Serviços de Oficina Mecânica, desenvolvida com base nos princípios de **Domain-Driven Design (DDD)** e **Arquitetura Hexagonal**.

**Base URL:** `http://localhost:8080`

**Versão:** v1.0.0

---

## Autenticação

Todos os endpoints (exceto login e acompanhamento de OS) requerem autenticação JWT.

### Como obter um token

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```json
{
  "usuario": "admin",
  "senha": "admin123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Como usar o token

Inclua no header de todas as requisições:
```
Authorization: Bearer {token}
```

**Validade do token:** 8 horas

---

## Endpoints

### Autenticação

#### POST `/api/v1/auth/login`

Realiza login e retorna um token JWT.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| usuario | String | Sim | Usuário de acesso |
| senha | String | Sim | Senha de acesso |

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response 403:** Credenciais inválidas

---

### Clientes

#### POST `/api/v1/clientes`

Cadastra um novo cliente no sistema.

| Campo | Tipo | Obrigatório | Validação | Exemplo |
|-------|------|-------------|-----------|---------|
| nome | String | Sim | 3-150 caracteres | "João da Silva" |
| documento | String | Sim | 11 (CPF) ou 14 (CNPJ) dígitos | "12345678909" |
| email | String | Não | Formato válido | "joao@email.com" |
| telefone | String | Não | 10-11 dígitos | "11999998888" |

**Response 201:**
```json
{
  "id": 1,
  "nome": "João da Silva",
  "documento": "12345678909",
  "email": "joao.silva@email.com",
  "telefone": "11999998888"
}
```

**Response 400:** Dados inválidos  
**Response 409:** CPF/CNPJ já cadastrado

---

#### GET `/api/v1/clientes`

Lista todos os clientes cadastrados.

**Response 200:**
```json
[
  {
    "id": 1,
    "nome": "João da Silva",
    "documento": "12345678909",
    "email": "joao@email.com",
    "telefone": "11999998888"
  }
]
```

---

#### GET `/api/v1/clientes/documento/{documento}`

Busca um cliente pelo CPF ou CNPJ.

**Parâmetro de rota:**
| Campo | Tipo | Descrição |
|-------|------|-----------|
| documento | String | CPF (11 dígitos) ou CNPJ (14 dígitos) |

**Response 200:** Cliente encontrado  
**Response 404:** Cliente não encontrado

---

### Veículos

#### POST `/api/v1/veiculos`

Cadastra um novo veículo vinculado a um cliente.

| Campo | Tipo | Obrigatório | Validação | Exemplo |
|-------|------|-------------|-----------|---------|
| marca | String | Sim | 2-50 caracteres | "Toyota" |
| modelo | String | Sim | 2-100 caracteres | "Corolla" |
| ano | Integer | Sim | 1900-2030 | 2022 |
| placa | String | Sim | Formato válido | "ABC1D23" |
| clienteId | Long | Sim | Deve existir | 1 |

**Formatos de placa aceitos:**
- Formato antigo: `ABC1234`
- Formato Mercosul: `ABC1D23`

**Response 201:**
```json
{
  "id": 1,
  "marca": "Toyota",
  "modelo": "Corolla",
  "ano": 2022,
  "placa": "ABC1D23",
  "clienteId": 1
}
```

**Response 400:** Dados inválidos  
**Response 409:** Placa já cadastrada

---

#### GET `/api/v1/veiculos`

Lista todos os veículos cadastrados.

---

#### GET `/api/v1/veiculos/cliente/{clienteId}`

Lista todos os veículos de um cliente específico.

---

#### GET `/api/v1/veiculos/placa/{placa}`

Busca um veículo pela placa.

**Parâmetro de rota:**
| Campo | Tipo | Descrição |
|-------|------|-----------|
| placa | String | Placa do veículo (ABC1234 ou ABC1D23) |

---

### Serviços

#### POST `/api/v1/servicos`

Cadastra um novo serviço no catálogo.

| Campo | Tipo | Obrigatório | Validação | Exemplo |
|-------|------|-------------|-----------|---------|
| nome | String | Sim | 3-150 caracteres | "Troca de Óleo" |
| descricao | String | Não | Até 500 caracteres | "Troca de óleo 1.0" |
| preco | BigDecimal | Sim | > 0 | 150.00 |
| tipo | String | Sim | Enum válido | "MANUTENCAO" |
| tempoEstimadoMinutos | Integer | Sim | >= 1 | 60 |

**Tipos de serviço disponíveis:**
| Valor | Descrição |
|-------|-----------|
| REVISAO | Revisão geral |
| MANUTENCAO | Manutenção preventiva/corretiva |
| TROCA_PECA | Troca de peças |
| ALINHAMENTO | Alinhamento de direção |
| BALANCEAMENTO | Balanceamento de rodas |
| MECANICA_GERAL | Mecânica em geral |
| ELETRICA | Serviços elétricos |
| SUSPENSAO | Serviços de suspensão |
| FREIOS | Serviços de freios |

**Response 201:**
```json
{
  "id": 1,
  "nome": "Troca de Óleo",
  "descricao": "Troca de óleo do motor 1.0 com filtro",
  "preco": 150.00,
  "tipo": "MANUTENCAO",
  "tempoEstimadoMinutos": 60
}
```

---

#### GET `/api/v1/servicos`

Lista todos os serviços do catálogo.

---

#### GET `/api/v1/servicos/{id}`

Busca um serviço pelo ID.

---

### Peças/Insumos

#### POST `/api/v1/pecas`

Cadastra uma nova peça no estoque.

| Campo | Tipo | Obrigatório | Validação | Exemplo |
|-------|------|-------------|-----------|---------|
| nome | String | Sim | 3-150 caracteres | "Filtro de Óleo" |
| descricao | String | Não | Até 500 caracteres | "Filtro motor 1.0" |
| codigo | String | Sim | 2-50 caracteres (único) | "FIL001" |
| preco | BigDecimal | Sim | > 0 | 45.90 |
| quantidadeEstoque | Integer | Sim | >= 0 | 50 |
| estoqueMinimo | Integer | Sim | >= 0 | 10 |

**Response 201:**
```json
{
  "id": 1,
  "nome": "Filtro de Óleo",
  "descricao": "Filtro de óleo para motor 1.0",
  "codigo": "FIL001",
  "preco": 45.90,
  "quantidadeEstoque": 50,
  "estoqueMinimo": 10,
  "estoqueBaixo": false
}
```

**Response 409:** Código já cadastrado

---

#### GET `/api/v1/pecas`

Lista todas as peças do estoque.

---

#### GET `/api/v1/pecas/{id}`

Busca uma peça pelo ID.

---

#### GET `/api/v1/pecas/codigo/{codigo}`

Busca uma peça pelo código interno.

---

#### PUT `/api/v1/pecas/{id}/repor-estoque/{quantidade}`

Adiciona quantidade ao estoque de uma peça.

**Parâmetros de rota:**
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long | ID da peça |
| quantidade | int | Quantidade a adicionar |

**Response 200:** Estoque reposto com sucesso  
**Response 404:** Peça não encontrada

---

#### GET `/api/v1/pecas/estoque-baixo`

Lista peças com estoque igual ou abaixo do mínimo configurado.

---

### Ordens de Serviço (OS)

#### POST `/api/v1/os`

Cria uma nova Ordem de Serviço.

| Campo | Tipo | Obrigatório | Exemplo |
|-------|------|-------------|---------|
| clienteId | Long | Sim | 1 |
| veiculoId | Long | Sim | 1 |
| dataPrevistaEntrega | DateTime | Não | "2026-08-20T18:00:00" |
| observacoes | String | Não | "Barulho no freio" |
| servicos | Array | Não | [见 exemplo abaixo] |
| pecas | Array | Não | [见 exemplo abaixo] |

**Exemplo de request completo:**
```json
{
  "clienteId": 1,
  "veiculoId": 1,
  "dataPrevistaEntrega": "2026-08-20T18:00:00",
  "observacoes": "Cliente relata barulho no freio dianteiro",
  "servicos": [
    {
      "servicoId": 1,
      "quantidade": 1
    }
  ],
  "pecas": [
    {
      "pecaId": 1,
      "quantidade": 2
    }
  ]
}
```

**Response 201:**
```json
{
  "id": 1,
  "clienteId": 1,
  "veiculoId": 1,
  "status": "RECEBIDA",
  "dataAbertura": "2026-08-12T10:30:00",
  "dataPrevistaEntrega": "2026-08-20T18:00:00",
  "observacoes": "Cliente relata barulho no freio",
  "valorServicos": 0.00,
  "valorPecas": 0.00,
  "valorTotal": 0.00,
  "servicos": [],
  "pecas": []
}
```

---

#### GET `/api/v1/os`

Lista todas as OS do sistema (uso administrativo).

---

#### GET `/api/v1/os/{id}`

Busca uma OS pelo ID com todos os detalhes.

**Response 200:**
```json
{
  "id": 1,
  "clienteId": 1,
  "veiculoId": 1,
  "status": "EM_EXECUCAO",
  "dataAbertura": "2026-08-12T10:30:00",
  "dataPrevistaEntrega": "2026-08-20T18:00:00",
  "dataConclusao": null,
  "observacoes": "Cliente relata barulho no freio",
  "valorServicos": 150.00,
  "valorPecas": 91.80,
  "valorTotal": 241.80,
  "servicos": [
    {
      "servicoId": 1,
      "nomeServico": "Troca de Óleo",
      "quantidade": 1,
      "precoUnitario": 150.00,
      "valorTotal": 150.00
    }
  ],
  "pecas": [
    {
      "pecaId": 1,
      "nomePeca": "Filtro de Óleo",
      "codigoPeca": "FIL001",
      "quantidade": 2,
      "precoUnitario": 45.90,
      "valorTotal": 91.80
    }
  ]
}
```

---

#### GET `/api/v1/os/cliente/{clienteId}`

Lista todas as OS de um cliente específico.

---

#### GET `/api/v1/os/status/{status}`

Lista todas as OS com um status específico.

**Status disponíveis:**
| Status | Descrição |
|--------|-----------|
| RECEBIDA | OS criada, aguardando processamento |
| EM_ANDAMENTO | OS em análise/andamento |
| AGUARDANDO_APROVACAO | Orçamento enviado ao cliente |
| APROVADA | Cliente aprovou o orçamento |
| EM_EXECUCAO | Serviço em execução |
| CONCLUIDA | Serviço concluído |
| ENTREGUE | Veículo entregue ao cliente |
| CANCELADA | OS cancelada |

---

#### GET `/api/v1/os/periodo`

Lista OS abertas em um período específico.

**Query Parameters:**
| Campo | Tipo | Formato | Exemplo |
|-------|------|---------|---------|
| inicio | DateTime | ISO 8601 | "2026-08-01T00:00:00" |
| fim | DateTime | ISO 8601 | "2026-08-31T23:59:59" |

---

#### PATCH `/api/v1/os/{id}/orcamento`

Envia o orçamento da OS para aprovação do cliente.

**Regras:**
- A OS deve estar com status `EM_ANDAMENTO`
- Deve ter pelo menos um serviço vinculado

**Response 200:** OS com status `AGUARDANDO_APROVACAO`  
**Response 400:** Transição inválida  
**Response 404:** OS não encontrada

---

#### PATCH `/api/v1/os/{id}/status/{status}`

Atualiza o status da OS conforme a máquina de estados.

**Máquina de Estados:**

```
RECEBIDA → EM_ANDAMENTO → AGUARDANDO_APROVACAO → APROVADA → EM_EXECUCAO → CONCLUIDA → ENTREGUE
```

**Transições válidas:**

| Status Atual | Próximos Status Válidos |
|--------------|-------------------------|
| RECEBIDA | EM_ANDAMENTO, CANCELADA |
| EM_ANDAMENTO | AGUARDANDO_APROVACAO, CANCELADA |
| AGUARDANDO_APROVACAO | APROVADA, CANCELADA |
| APROVADA | EM_EXECUCAO, CANCELADA |
| EM_EXECUCAO | CONCLUIDA |
| CONCLUIDA | ENTREGUE |
| ENTREGUE | *(nenhum)* |
| CANCELADA | *(nenhum)* |

**Response 200:** Status atualizado  
**Response 400:** Transição inválida  
**Response 404:** OS não encontrada

---

#### GET `/api/v1/os/{id}/acompanhar`

**Endpoint PÚBLICO** (não requer autenticação)

Permite ao cliente acompanhar o progresso da OS.

**Response 200:**
```json
{
  "id": 1,
  "status": "EM_EXECUCAO",
  "dataAbertura": "2026-08-12T10:30:00",
  "dataPrevistaEntrega": "2026-08-20T18:00:00",
  "valorTotal": 241.80
}
```

---

## Formato de Datas

Todos os campos de data utilizam o formato **ISO 8601:**

```
AAAA-MM-DDTHH:mm:ss
```

**Exemplo:** `2026-08-12T18:30:00`

---

## Respostas de Erro

Padrão de erro da API:

```json
{
  "timestamp": "2026-08-12T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "O nome é obrigatório",
  "path": "/api/v1/clientes"
}
```

---

## Códigos de Resposta HTTP

| Código | Descrição |
|--------|-----------|
| 200 | Requisição bem-sucedida |
| 201 | Recurso criado com sucesso |
| 400 | Dados inválidos na requisição |
| 403 | Acesso negado / Credenciais inválidas |
| 404 | Recurso não encontrado |
| 409 | Conflito (recurso já existe) |
| 500 | Erro interno do servidor |

---

## Swagger UI

Acesse a documentação interativa em:

```
http://localhost:8080/swagger-ui.html
```

---

## Exemplos de Uso Completo

### Fluxo Completo de uma OS

#### 1. Cadastrar Cliente
```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "nome": "João da Silva",
    "documento": "12345678909",
    "email": "joao@email.com",
    "telefone": "11999998888"
  }'
```

#### 2. Cadastrar Veículo
```bash
curl -X POST http://localhost:8080/api/v1/veiculos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "marca": "Toyota",
    "modelo": "Corolla",
    "ano": 2022,
    "placa": "ABC1D23",
    "clienteId": 1
  }'
```

#### 3. Criar Ordem de Serviço
```bash
curl -X POST http://localhost:8080/api/v1/os \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "clienteId": 1,
    "veiculoId": 1,
    "observacoes": "Troca de óleo e filtro",
    "servicos": [{"servicoId": 1, "quantidade": 1}],
    "pecas": [{"pecaId": 1, "quantidade": 1}]
  }'
```

#### 4. Enviar para Aprovação
```bash
curl -X PATCH http://localhost:8080/api/v1/os/1/orcamento \
  -H "Authorization: Bearer {token}"
```

#### 5. Aprovar Orçamento
```bash
curl -X PATCH http://localhost:8080/api/v1/os/1/status/APROVADA \
  -H "Authorization: Bearer {token}"
```

#### 6. Iniciar Execução
```bash
curl -X PATCH http://localhost:8080/api/v1/os/1/status/EM_EXECUCAO \
  -H "Authorization: Bearer {token}"
```

#### 7. Concluir Serviço
```bash
curl -X PATCH http://localhost:8080/api/v1/os/1/status/CONCLUIDA \
  -H "Authorization: Bearer {token}"
```

#### 8. Entregar ao Cliente
```bash
curl -X PATCH http://localhost:8080/api/v1/os/1/status/ENTREGUE \
  -H "Authorization: Bearer {token}"
```
