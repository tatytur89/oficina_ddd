package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.domain.valueobjects.TipoServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AberturaOrdemServicoDomainTest {
    private final Servico servicoGratuito = new Servico(3L, "Inspecao", "Cortesia", new Preco(0), TipoServico.REVISAO, 30);
    private final Peca pecaGratuita = new Peca(4L, "Brinde", "Cortesia", "B001", new Preco(0), 10, 1);

    private void assertSemItens(OrdemServico os) {
        assertTrue(os.getServicos().isEmpty());
        assertTrue(os.getPecas().isEmpty());
        assertEquals(new Preco(BigDecimal.ZERO), os.getValorTotal());
    }

    @Test
    void registraItensSolicitadosSomenteAntesDaPersistenciaEDoDiagnostico() {
        var os = OrdemServico.abrir(1L, 2L, "Revisao");
        os.registrarServicoSolicitado(servicoGratuito, 1);
        os.registrarPecaSolicitada(pecaGratuita, 2);
        assertNull(os.getId());
        assertEquals(StatusOS.RECEBIDA, os.getStatus());
        assertEquals(1, os.getServicos().getFirst().getQuantidade());
        assertEquals(2, os.getPecas().getFirst().getQuantidade());
        assertEquals(10, pecaGratuita.getQuantidadeEstoque());
    }

    @Test
    void rejeitaRegistroDeItensSolicitadosEmOrdemJaPersistidaMesmoRecebida() {
        var zero = new Preco(0);
        var os = new OrdemServico(42L, 1L, 2L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null,
                "obs", zero, zero, zero, null, null, "chave", null, null);
        assertThrows(IllegalStateException.class, () -> os.registrarServicoSolicitado(servicoGratuito, 1));
        assertThrows(IllegalStateException.class, () -> os.registrarPecaSolicitada(pecaGratuita, 1));
        assertSemItens(os);
    }

    @Test
    void rejeitaRegistroDeAberturaDepoisDeIniciarDiagnosticoMesmoSemId() {
        var os = OrdemServico.abrir(1L, 2L, null);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        assertThrows(IllegalStateException.class, () -> os.registrarServicoSolicitado(servicoGratuito, 1));
        assertThrows(IllegalStateException.class, () -> os.registrarPecaSolicitada(pecaGratuita, 1));
        assertSemItens(os);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeInvalidaDeServicoSolicitadoMesmoComPrecoZero(int quantidade) {
        var os = OrdemServico.abrir(1L, 2L, null);
        assertThrows(IllegalArgumentException.class, () -> os.registrarServicoSolicitado(servicoGratuito, quantidade));
        assertSemItens(os);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeInvalidaDePecaSolicitadaMesmoComPrecoZero(int quantidade) {
        var os = OrdemServico.abrir(1L, 2L, null);
        assertThrows(IllegalArgumentException.class, () -> os.registrarPecaSolicitada(pecaGratuita, quantidade));
        assertSemItens(os);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeInvalidaDeServicoNoDiagnosticoMesmoComPrecoZero(int quantidade) {
        var os = OrdemServico.abrir(1L, 2L, null);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        assertThrows(IllegalArgumentException.class, () -> os.adicionarServico(servicoGratuito, quantidade));
        assertSemItens(os);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeInvalidaDePecaNoDiagnosticoMesmoComPrecoZero(int quantidade) {
        var os = OrdemServico.abrir(1L, 2L, null);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        assertThrows(IllegalArgumentException.class, () -> os.adicionarPeca(pecaGratuita, quantidade));
        assertSemItens(os);
    }
}
