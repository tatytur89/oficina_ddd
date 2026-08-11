package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenResponseDTO {
@Schema(description = "Token JWT gerado para autenticação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    public TokenResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
