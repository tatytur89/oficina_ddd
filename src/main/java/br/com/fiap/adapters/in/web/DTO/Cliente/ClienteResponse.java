package br.com.fiap.adapters.in.web.DTO.Cliente;

import br.com.fiap.domain.entities.Cliente;

public record ClienteResponse(
        Long id,
        String nome,
        String documento,
        String email,
        String telefone
) {
    public ClienteResponse(Cliente cliente) {
        this(
            cliente.getId(),
            cliente.getNome(),
            cliente.getDocumento(),
            cliente.getEmail(),
            cliente.getTelefone()
        );
    }
}