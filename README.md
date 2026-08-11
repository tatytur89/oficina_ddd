# Oficina DDD

Projeto Spring Boot desenvolvido para o Tech Challenge da FIAP.

## Descrição

Este projeto é uma aplicação Java com Spring Boot, utilizando Maven Wrapper para facilitar a execução sem a necessidade de instalar Maven globalmente.

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
http://localhost:8080
```

## Rodar testes

Para executar a suíte de testes do projeto:

```bash
./mvnw test
```

## Interface Swagger

A documentação interativa da API pode ser acessada em:

```text
http://localhost:8080/swagger-ui/index.html
```

## Endpoint de login

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
- `403 Forbidden` quando as credenciais são inválidas

Exemplo de resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Usar o token JWT

Após autenticar, use o token retornado no header `Authorization` das requisições protegidas:

```text
Authorization: Bearer <token>
```

No Swagger UI, clique em `Authorize`, cole o token JWT e confirme. Em seguida, utilize o botão `Try it out` nos endpoints protegidos.

## Endpoints da API de clientes

### Cadastrar cliente

- Método: `POST`
- URL: `/api/v1/clientes`
- Content-Type: `application/json`

Exemplo de corpo:

```json
{
  "nome": "João da Silva",
  "documento": "12345678909",
  "email": "joao@example.com",
  "telefone": "11999999999"
}
```

Retorna:

- `201 Created` quando o cliente é cadastrado com sucesso
- `400 Bad Request` quando os dados são inválidos

### Listar todos os clientes

- Método: `GET`
- URL: `/api/v1/clientes`

Retorna:

- `200 OK` com a lista de clientes cadastrados

### Buscar cliente por CPF ou CNPJ

- Método: `GET`
- URL: `/api/v1/clientes/documento/{documento}`

Exemplo:

```text
GET /api/v1/clientes/documento/12345678909
```

Retorna:

- `200 OK` com os dados do cliente encontrado
- `404 Not Found` quando não há cliente com o documento informado

