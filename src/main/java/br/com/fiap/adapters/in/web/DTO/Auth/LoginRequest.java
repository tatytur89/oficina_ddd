package br.com.fiap.adapters.in.web.DTO.Auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso para autenticação")
public record LoginRequest(

    @Schema(example = "admin")
    @NotBlank(message = "O usuário é obrigatório")
    String usuario,

    @Schema(example = "admin123")
    @NotBlank(message = "A senha é obrigatória")
    String senha

) {}
