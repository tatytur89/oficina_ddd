package br.com.fiap.adapters.in.web;

import br.com.fiap.domain.entities.Cliente;
import jakarta.validation.constraints.NotBlank;

public class ClienteResponseDTO {
@NotBlank(message = "O nome é obrigatório")
private Long id;
    private String nome;
    private String documento;
    private String email;
    private String telefone;

    // Construtor que facilita o mapeamento a partir da entidade de domínio
    public ClienteResponseDTO(Cliente cliente) {
        this.id = cliente.getId();
        this.nome = cliente.getNome();
        this.documento = cliente.getDocumento();
        this.email = cliente.getEmail();
        this.telefone = cliente.getTelefone();
    }

    // Getters
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
}
