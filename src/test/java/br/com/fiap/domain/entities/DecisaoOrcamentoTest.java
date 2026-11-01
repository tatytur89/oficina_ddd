package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.domain.valueobjects.TipoServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DecisaoOrcamentoTest {
    private OrdemServico aguardando() {
        var zero = new Preco(BigDecimal.ZERO);
        var os = new OrdemServico(1L, 1L, 2L, StatusOS.EM_DIAGNOSTICO, LocalDateTime.now(), null, null,
                "Freios", zero, zero, zero, null, null, "chave-secreta", null, null);
        os.adicionarServico(new Servico(3L, "Freios", "Revisao", new Preco(100), TipoServico.MANUTENCAO, 60), 1);
        os.adicionarPeca(new Peca(4L, "Pastilha", "Freios", "P001", new Preco(25), 10, 1), 2);
        os.enviarOrcamento();
        return os;
    }

    @Test
    void enviarOrcamentoCriaVersaoEDeixaDecisaoPendente() {
        var os = aguardando();
        assertEquals(1L, os.getVersaoOrcamento());
        assertNull(os.getDecisaoOrcamento());
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    void aprovaComChaveEVersaoCorretas() {
        var os = aguardando();
        assertTrue(os.decidirOrcamento("chave-secreta", 1L, true));
        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
        assertEquals(Boolean.TRUE, os.getDecisaoOrcamento());
        assertEquals(1L, os.getVersaoOrcamento());
    }

    @Test
    void recusaRetornaAoDiagnosticoPreservandoItensEValores() {
        var os = aguardando();
        var total = os.getValorTotal();
        assertTrue(os.decidirOrcamento("chave-secreta", 1L, false));
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
        assertEquals(Boolean.FALSE, os.getDecisaoOrcamento());
        assertEquals(1, os.getServicos().size());
        assertEquals(1, os.getPecas().size());
        assertEquals(total, os.getValorTotal());
    }

    @Test
    void repeticaoDaMesmaAprovacaoNaoAlteraOrdem() {
        var os = aguardando();
        assertTrue(os.decidirOrcamento("chave-secreta", 1L, true));
        assertFalse(os.decidirOrcamento("chave-secreta", 1L, true));
        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
    }

    @Test
    void repeticaoDaMesmaRecusaNaoAlteraOrdem() {
        var os = aguardando();
        assertTrue(os.decidirOrcamento("chave-secreta", 1L, false));
        assertFalse(os.decidirOrcamento("chave-secreta", 1L, false));
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
    }

    @Test
    void rejeitaDecisaoContraditoriaNaMesmaVersao() {
        var os = aguardando();
        os.decidirOrcamento("chave-secreta", 1L, false);
        assertThrows(IllegalStateException.class, () -> os.decidirOrcamento("chave-secreta", 1L, true));
        assertEquals(Boolean.FALSE, os.getDecisaoOrcamento());
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
    }

    @Test
    void novoEnvioAposRecusaIncrementaVersaoELimpaDecisao() {
        var os = aguardando();
        os.decidirOrcamento("chave-secreta", 1L, false);
        os.enviarOrcamento();
        assertEquals(2L, os.getVersaoOrcamento());
        assertNull(os.getDecisaoOrcamento());
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
        assertThrows(IllegalStateException.class, () -> os.decidirOrcamento("chave-secreta", 1L, true));
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
        assertTrue(os.decidirOrcamento("chave-secreta", 2L, true));
    }

    @Test
    void rejeitaVersaoFuturaOuAntigaAntesDeMudarEstado() {
        var os = aguardando();
        assertThrows(IllegalStateException.class, () -> os.decidirOrcamento("chave-secreta", 0L, true));
        assertThrows(IllegalStateException.class, () -> os.decidirOrcamento("chave-secreta", 2L, true));
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
        assertNull(os.getDecisaoOrcamento());
    }

    @Test
    void chaveErradaNaoAprovaNemMesmoRepeticao() {
        var os = aguardando();
        assertThrows(IllegalArgumentException.class, () -> os.decidirOrcamento("errada", 1L, true));
        os.decidirOrcamento("chave-secreta", 1L, true);
        assertThrows(IllegalArgumentException.class, () -> os.decidirOrcamento("errada", 1L, true));
    }

    @Test
    void patchGenericoNaoPodeAprovar() {
        var os = aguardando();
        assertThrows(IllegalStateException.class, () -> os.transicionarPara(StatusOS.EM_EXECUCAO));
        assertNull(os.getDecisaoOrcamento());
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    void patchGenericoDeEnvioNaoPodeContornarValidacaoDoOrcamento() {
        var zero = new Preco(BigDecimal.ZERO);
        var os = new OrdemServico(1L, 1L, 2L, StatusOS.EM_DIAGNOSTICO, LocalDateTime.now(), null, null,
                null, zero, zero, zero, null, null, "chave", null, null);
        assertThrows(IllegalStateException.class, () -> os.transicionarPara(StatusOS.AGUARDANDO_APROVACAO));
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
    }

    @Test
    void replayDaAprovacaoDepoisDeFinalizadaNaoReabreOrdem() {
        var os = aguardando();
        os.decidirOrcamento("chave-secreta", 1L, true);
        os.transicionarPara(StatusOS.FINALIZADA);
        assertFalse(os.decidirOrcamento("chave-secreta", 1L, true));
        assertEquals(StatusOS.FINALIZADA, os.getStatus());
    }
}
