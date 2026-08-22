package br.com.fiap.adapters.in.web.mapper;

import org.junit.jupiter.api.Test;

import br.com.fiap.adapters.in.web.DTO.Peca.PecaCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaResponse;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaUpdateRequest;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PecaMapperTest {

    @Test
    void deveMapearCreateRequestParaDominio() {
        PecaCreateRequest dto = new PecaCreateRequest("Filtro de Óleo", "Descrição", "FIL001", BigDecimal.valueOf(45.90), 50, 10);

        Peca peca = PecaMapper.toDomain(dto);

        assertNull(peca.getId());
        assertEquals("Filtro de Óleo", peca.getNome());
        assertEquals("FIL001", peca.getCodigo());
        assertEquals(50, peca.getQuantidadeEstoque());
    }

    @Test
    void deveMapearUpdateRequestParaDominioComIdEPreservarQuantidadeNula() {
        PecaUpdateRequest dto = new PecaUpdateRequest("Filtro de Óleo Premium", "Descrição", "FIL001", BigDecimal.valueOf(55.00), 15);

        Peca peca = PecaMapper.toDomain(1L, dto);

        assertEquals(1L, peca.getId());
        assertEquals("Filtro de Óleo Premium", peca.getNome());
        assertNull(peca.getQuantidadeEstoque());
        assertEquals(15, peca.getEstoqueMinimo());
    }

    @Test
    void deveMapearDominioParaResponse() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL001", new Preco(BigDecimal.valueOf(45.90)), 50, 10);

        PecaResponse response = PecaMapper.toResponse(peca);

        assertEquals(1L, response.id());
        assertEquals("FIL001", response.codigo());
        assertFalse(response.estoqueBaixo());
    }

    @Test
    void deveMapearListaDeDominioParaResponse() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL001", new Preco(BigDecimal.valueOf(45.90)), 50, 10);

        List<PecaResponse> responses = PecaMapper.toResponseList(List.of(peca));

        assertEquals(1, responses.size());
        assertEquals("FIL001", responses.get(0).codigo());
    }
}
