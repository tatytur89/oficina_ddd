package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.ClienteRequestDTO;
import br.com.fiap.adapters.in.web.ClienteResponseDTO;
import br.com.fiap.domain.entities.Cliente;

public class ClienteMapper {
	
	public static Cliente toDomain(Long id, ClienteRequestDTO dto) {
        return new Cliente(
                id,
                dto.getNome(),
                dto.getDocumento(),
                dto.getEmail(),
                dto.getTelefone()
        );
    }
	
	public static List<ClienteResponseDTO> toResponseList(List<Cliente> clientes) {
	    return clientes.stream()
	            .map(ClienteMapper::toResponse)
	            .toList();
	}

    public static ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(cliente);
    }

}
