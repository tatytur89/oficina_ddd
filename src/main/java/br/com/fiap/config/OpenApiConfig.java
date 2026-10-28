package br.com.fiap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .components(components())
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
            .title("Oficina Mecânica API")
            .description(buildDescription())
            .version("v1.0.0")
            .contact(new Contact()
                .name("Equipe de Desenvolvimento")
                .email("suporte@oficinamecanica.com.br")
                .url("https://github.com/fiap/oficina-ddd"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    private String buildDescription() {
        return """
            # Oficina Mecânica API
            
            ## Visão Geral
            API RESTful para o Sistema Integrado de Atendimento e Execução de Serviços de Oficina Mecânica,
            desenvolvida com base nos princípios de **Domain-Driven Design (DDD)** e **Arquitetura Hexagonal**.
            
            ## Funcionalidades
            - **Gestão de Clientes**: Cadastro e consulta de clientes (CPF/CNPJ)
            - **Gestão de Veículos**: Cadastro de veículos vinculados a clientes
            - **Catálogo de Serviços**: Cadastro de serviços com precificação
            - **Estoque de Peças**: Controle de peças e insumos com baixa automática
            - **Ordens de Serviço**: Fluxo completo com máquina de estados
            
            ## Autenticação
            As rotas administrativas requerem JWT. Acompanhamento e decisões externas de orçamento
            usam a chave de acesso da OS; a decisão também exige a versão do orçamento.
            Login, informações da aplicação e saúde possuem acesso público.
            
            ### Como obter um token:
            1. Acesse `POST /api/v1/auth/login` com suas credenciais
            2. Use o token retornado no header `Authorization: Bearer <token>`
            
            ## Máquina de Estados da OS
            ```
            Recebida → Em Diagnóstico → Aguardando Aprovação → Em Execução → Finalizada → Entregue
            ```
            A aprovação inicia a execução e baixa estoque. A recusa retorna ao diagnóstico,
            preservando os itens para revisão. Cada reenvio gera uma nova versão do orçamento.
            
            ## Formato de Datas
            Todos os campos de data utilizam o formato ISO 8601: `AAAA-MM-DDTHH:mm:ss`
            
            ## Fila Operacional
            Prioridade: Execução, Aguardando Aprovação, Diagnóstico, Recebida.
            Mais antigas primeiro dentro do status; finalizadas e entregues permanecem no histórico.
            """;
    }

    private Components components() {
        return new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Insira o token JWT obtido no endpoint de login"));
    }
}
