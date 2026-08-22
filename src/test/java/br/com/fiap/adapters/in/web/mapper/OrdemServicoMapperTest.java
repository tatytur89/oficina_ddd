package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest.ItemPecaRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest.ItemServicoRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoResponse;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.ItemQuantidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoMapperTest {

    @Test
    void deveMapearCreateRequestParaDominio() {
        OrdemServicoCreateRequest dto = new OrdemServicoCreateRequest(
                1L, 1L, LocalDateTime.of(2026, 8, 30, 18, 0), "Barulho no freio",
                List.of(new ItemServicoRequest(1L, 1)),
                List.of(new ItemPecaRequest(1L, 2))
        );

        OrdemServico os = OrdemServicoMapper.toDomain(dto);

        assertNull(os.getId());
        assertEquals(1L, os.getClienteId());
        assertEquals(1L, os.getVeiculoId());
        assertEquals("Barulho no freio", os.getObservacoes());
    }

    @Test
    void deveMapearItensDeServicoDaRequisicao() {
        OrdemServicoCreateRequest dto = new OrdemServicoCreateRequest(
                1L, 1L, null, "obs",
                List.of(new ItemServicoRequest(1L, 2)),
                null
        );

        List<ItemQuantidade> itens = OrdemServicoMapper.toItensServicos(dto);

        assertEquals(1, itens.size());
        assertEquals(1L, itens.get(0).id());
        assertEquals(2, itens.get(0).quantidade());
    }

    @Test
    void deveRetornarListaVaziaQuandoServicosForNull() {
        OrdemServicoCreateRequest dto = new OrdemServicoCreateRequest(1L, 1L, null, "obs", null, null);

        assertTrue(OrdemServicoMapper.toItensServicos(dto).isEmpty());
        assertTrue(OrdemServicoMapper.toItensPecas(dto).isEmpty());
    }

    @Test
    void deveMapearItensDePecaDaRequisicao() {
        OrdemServicoCreateRequest dto = new OrdemServicoCreateRequest(
                1L, 1L, null, "obs",
                null,
                List.of(new ItemPecaRequest(2L, 3))
        );

        List<ItemQuantidade> itens = OrdemServicoMapper.toItensPecas(dto);

        assertEquals(1, itens.size());
        assertEquals(2L, itens.get(0).id());
        assertEquals(3, itens.get(0).quantidade());
    }

    @Test
    void deveMapearDominioParaResponse() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);

        OrdemServicoResponse response = OrdemServicoMapper.toResponse(os);

        assertEquals(1L, response.id());
        assertEquals("RECEBIDA", response.status());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);

        List<OrdemServicoResponse> responses = OrdemServicoMapper.toResponseList(List.of(os));

        assertEquals(1, responses.size());
    }
}
