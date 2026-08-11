package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClienteRequestDTO {
    @Schema(description = "Nome completo do cliente", example = "João da Silva")
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @Schema(description = "CPF ou CNPJ do cliente contendo apenas números", example = "12345678909")
    @NotBlank(message = "O documento é obrigatório")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "O documento deve conter 11 ou 14 dígitos numéricos")
    private String documento;

    @Schema(description = "E-mail de contato", example = "joao.silva@email.com")
    @Email(message = "Formato de e-mail inválido")
    private String email;
    
    @Schema(description = "Telefone para contato com DDD", example = "11999998888")
    private String telefone;

    // Construtores, Getters e Setters
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
