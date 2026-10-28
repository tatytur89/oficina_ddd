package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoResponse;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.CriarOrdemServicoCommand;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoMapperTest {

    @Test
    void deveMapearCreateRequestParaComando() {
        OrdemServicoCreateRequest dto = new OrdemServicoCreateRequest(
                1L, 1L, "Barulho no freio"
        );

        CriarOrdemServicoCommand comando = OrdemServicoMapper.toCommand(dto);

        assertEquals(1L, comando.clienteId());
        assertEquals(1L, comando.veiculoId());
        assertEquals("Barulho no freio", comando.observacoes());
        assertTrue(comando.servicos().isEmpty());
        assertTrue(comando.pecas().isEmpty());
    }

    @Test
    void deveMapearDominioParaResponse() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null, null, null, null);

        OrdemServicoResponse response = OrdemServicoMapper.toResponse(os);

        assertEquals(1L, response.id());
        assertEquals("RECEBIDA", response.status());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null, null, null, null);

        List<OrdemServicoResponse> responses = OrdemServicoMapper.toResponseList(List.of(os));

        assertEquals(1, responses.size());
    }
}
