package br.com.fiap.adapters.in.web;

import br.com.fiap.domain.entities.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um cliente")
public class ClienteResponseDTO {

    @Schema(
        description = "Identificador único do cliente",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "Nome completo do cliente",
        example = "João da Silva"
    )
    private String nome;

    @Schema(
        description = "CPF ou CNPJ do cliente (apenas números)",
        example = "12345678909"
    )
    private String documento;

    @Schema(
        description = "E-mail de contato",
        example = "joao.silva@email.com",
        format = "email"
    )
    private String email;

    @Schema(
        description = "Telefone para contato com DDD",
        example = "11999998888"
    )
    private String telefone;

    public ClienteResponseDTO(Cliente cliente) {
        this.id = cliente.getId();
        this.nome = cliente.getNome();
        this.documento = cliente.getDocumento();
        this.email = cliente.getEmail();
        this.telefone = cliente.getTelefone();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
}
