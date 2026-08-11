package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {
@Schema(description = "Usuário de acesso do administrador", example = "admin")
    @NotBlank(message = "O usuário é obrigatório")
    private String usuario;

    @Schema(description = "Senha de acesso", example = "admin123")
    @NotBlank(message = "A senha é obrigatória")
    private String senha;

    // Getters e Setters
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
