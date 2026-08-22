package br.com.fiap.adapters.in.web.DTO.Auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta com o token JWT gerado")
public record TokenResponse(
        @Schema(
            description = "Token JWT para autenticação nas requisições. Use no header: Authorization: Bearer {token}",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY5MTg5NjAwMCwiZXhwIjoxNjkxOTgyNDAwfQ.example"
        )
        String token
) {}
