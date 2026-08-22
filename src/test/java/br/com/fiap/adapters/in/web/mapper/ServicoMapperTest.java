package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.Servico.ServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoResponse;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoUpdateRequest;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicoMapperTest {

    @Test
    void deveMapearCreateRequestParaDominio() {
        ServicoCreateRequest dto = new ServicoCreateRequest("Troca de Óleo", "Descrição", BigDecimal.valueOf(150.00), "MANUTENCAO", 60);

        Servico servico = ServicoMapper.toDomain(dto);

        assertNull(servico.getId());
        assertEquals("Troca de Óleo", servico.getNome());
        assertEquals(TipoServico.MANUTENCAO, servico.getTipo());
    }

    @Test
    void deveMapearUpdateRequestParaDominioComId() {
        ServicoUpdateRequest dto = new ServicoUpdateRequest("Troca de Óleo Premium", "Descrição", BigDecimal.valueOf(200.00), "MANUTENCAO", 45);

        Servico servico = ServicoMapper.toDomain(1L, dto);

        assertEquals(1L, servico.getId());
        assertEquals("Troca de Óleo Premium", servico.getNome());
    }

    @Test
    void deveMapearDominioParaResponse() {
        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);

        ServicoResponse response = ServicoMapper.toResponse(servico);

        assertEquals(1L, response.id());
        assertEquals("Troca de Óleo", response.nome());
        assertEquals("MANUTENCAO", response.tipo());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);

        List<ServicoResponse> responses = ServicoMapper.toResponseList(List.of(servico));

        assertEquals(1, responses.size());
        assertEquals("Troca de Óleo", responses.get(0).nome());
    }
}
