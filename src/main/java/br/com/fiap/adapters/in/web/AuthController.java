package br.com.fiap.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.ports.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Autenticação",
    description = "Endpoints para autenticação e geração de tokens JWT"
)
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Operation(
        summary = "Realizar login",
        description = """
            Autentica um administrador do sistema e retorna um token JWT.
            
            **Uso:**
            1. Envie as credenciais (usuário e senha)
            2. Use o token retornado no header `Authorization: Bearer {token}`
            
            **Validade do token:** 8 horas
            
            **Credenciais padrão (seed):**
            - Usuário: `admin`
            - Senha: `admin123`
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY5MTg5NjAwMCwiZXhwIjoxNjkxOTgyNDAwfQ..."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Credenciais inválidas (corpo vazio)"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Credenciais de acesso",
                required = true
            )
            @Valid @RequestBody LoginRequestDTO request) {
        
        try {
            String token = authUseCase.autenticar(request.getUsuario(), request.getSenha());
            return ResponseEntity.ok(new TokenResponseDTO(token));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).build();
        }
    }
}
