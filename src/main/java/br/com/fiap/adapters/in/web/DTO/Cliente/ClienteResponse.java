package br.com.fiap.adapters.in.web.DTO.Cliente;

import br.com.fiap.domain.entities.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um cliente")
public record ClienteResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "João da Silva") String nome,
        @Schema(example = "12345678909") String documento,
        @Schema(example = "joao.silva@email.com") String email,
        @Schema(example = "11999998888") String telefone
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
