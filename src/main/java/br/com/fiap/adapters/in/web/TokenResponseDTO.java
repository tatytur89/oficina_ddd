package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta com o token JWT gerado")
public class TokenResponseDTO {

    @Schema(
        description = "Token JWT para autenticação nas requisições. Use no header: Authorization: Bearer {token}",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY5MTg5NjAwMCwiZXhwIjoxNjkxOTgyNDAwfQ.example"
    )
    private String token;

    public TokenResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
