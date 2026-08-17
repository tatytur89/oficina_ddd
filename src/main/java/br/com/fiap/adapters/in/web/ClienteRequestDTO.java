package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastro de um novo cliente")
public class ClienteRequestDTO {

    @Schema(
        description = "Nome completo do cliente",
        example = "João da Silva",
        minLength = 3,
        maxLength = 150,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Schema(
        description = "CPF ou CNPJ do cliente (apenas números). CPF: 11 dígitos, CNPJ: 14 dígitos",
        example = "12345678909",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O documento é obrigatório")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "O documento deve conter 11 dígitos (CPF) ou 14 dígitos (CNPJ)")
    private String documento;

    @Schema(
        description = "E-mail de contato do cliente",
        example = "joao.silva@email.com",
        format = "email"
    )
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(
        description = "Telefone para contato com DDD (apenas números)",
        example = "11999998888"
    )
    @Pattern(regexp = "\\d{10,11}", message = "O telefone deve conter 10 ou 11 dígitos")
    private String telefone;

    public ClienteRequestDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
