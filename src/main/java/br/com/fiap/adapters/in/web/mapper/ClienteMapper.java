package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteResponse;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteUpdateRequest;
import br.com.fiap.domain.entities.Cliente;

public class ClienteMapper {
	
	public static Cliente toDomain(ClienteCreateRequest dto) {
	    return new Cliente(null, dto.nome(), dto.documento(), dto.email(), dto.telefone());
	}

	public static Cliente toDomain(Long id, ClienteUpdateRequest dto) {
	    return new Cliente(id, dto.nome(), dto.documento(), dto.email(), dto.telefone());
	}

	public static ClienteResponse toClienteResponse(Cliente cliente) {
	    return new ClienteResponse(cliente);
	}

	public static List<ClienteResponse> toClienteResponseList(List<Cliente> clientes) {
	    return clientes.stream().map(ClienteMapper::toClienteResponse).toList();
	}

}
