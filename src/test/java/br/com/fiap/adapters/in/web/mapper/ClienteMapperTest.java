package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteResponse;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteUpdateRequest;
import br.com.fiap.domain.entities.Cliente;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteMapperTest {

    @Test
    void deveMapearCreateRequestParaDominio() {
        ClienteCreateRequest dto = new ClienteCreateRequest("Robert", "12345678909", "robert@email.com", "11999999999");

        Cliente cliente = ClienteMapper.toDomain(dto);

        assertNull(cliente.getId());
        assertEquals("Robert", cliente.getNome());
        assertEquals("12345678909", cliente.getDocumento());
        assertEquals("robert@email.com", cliente.getEmail());
        assertEquals("11999999999", cliente.getTelefone());
    }

    @Test
    void deveMapearUpdateRequestParaDominioComId() {
        ClienteUpdateRequest dto = new ClienteUpdateRequest("Robert Silva", "12345678909", "robert@email.com", "11999999999");

        Cliente cliente = ClienteMapper.toDomain(1L, dto);

        assertEquals(1L, cliente.getId());
        assertEquals("Robert Silva", cliente.getNome());
    }

    @Test
    void deveMapearDominioParaResponse() {
        Cliente cliente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");

        ClienteResponse response = ClienteMapper.toClienteResponse(cliente);

        assertEquals(1L, response.id());
        assertEquals("Robert", response.nome());
        assertEquals("12345678909", response.documento());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        Cliente cliente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");

        List<ClienteResponse> responses = ClienteMapper.toClienteResponseList(List.of(cliente));

        assertEquals(1, responses.size());
        assertEquals("Robert", responses.get(0).nome());
    }
}
