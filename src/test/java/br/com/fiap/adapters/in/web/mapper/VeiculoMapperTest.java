package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoResponse;
import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoUpdateRequest;
import br.com.fiap.domain.entities.Veiculo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VeiculoMapperTest {

    @Test
    void deveMapearCreateRequestParaDominio() {
        VeiculoCreateRequest dto = new VeiculoCreateRequest("Toyota", "Corolla", 2022, "ABC1D23", 1L);

        Veiculo veiculo = VeiculoMapper.toDomain(dto);

        assertNull(veiculo.getId());
        assertEquals("Toyota", veiculo.getMarca());
        assertEquals("ABC1D23", veiculo.getPlaca());
        assertEquals(1L, veiculo.getClienteId());
    }

    @Test
    void deveMapearUpdateRequestParaDominioComId() {
        VeiculoUpdateRequest dto = new VeiculoUpdateRequest("Toyota", "Corolla Cross", 2023, "ABC1D23", 1L);

        Veiculo veiculo = VeiculoMapper.toDomain(1L, dto);

        assertEquals(1L, veiculo.getId());
        assertEquals("Corolla Cross", veiculo.getModelo());
    }

    @Test
    void deveMapearDominioParaResponse() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

        VeiculoResponse response = VeiculoMapper.toResponse(veiculo);

        assertEquals(1L, response.id());
        assertEquals("Corolla", response.modelo());
        assertEquals("ABC1D23", response.placa());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

        List<VeiculoResponse> responses = VeiculoMapper.toResponseList(List.of(veiculo));

        assertEquals(1, responses.size());
        assertEquals("Corolla", responses.get(0).modelo());
    }
}
