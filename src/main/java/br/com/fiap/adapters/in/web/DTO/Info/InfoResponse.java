package br.com.fiap.adapters.in.web.DTO.Info;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações da versão da aplicação em execução")
public record InfoResponse(
        @Schema(description = "Nome da aplicação", example = "oficina-ddd")
        String aplicacao,

        @Schema(description = "Commit do Git que gerou a imagem em execução", example = "db9cdbd1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7")
        String commit,

        @Schema(description = "Nome do pod (hostname) que atendeu a requisição", example = "oficina-api-7c9f8d6b5-x2k4p")
        String pod
) {}
