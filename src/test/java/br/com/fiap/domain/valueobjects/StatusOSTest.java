package br.com.fiap.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class StatusOSTest {

    @Test
    void deveRetornarProximoStatusParaRecebida() {
        List<StatusOS> proximos = StatusOS.RECEBIDA.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.EM_DIAGNOSTICO));
    }

    @Test
    void deveRetornarProximoStatusParaEmDiagnostico() {
        List<StatusOS> proximos = StatusOS.EM_DIAGNOSTICO.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.AGUARDANDO_APROVACAO));
    }

    @Test
    void deveRetornarProximoStatusParaAguardandoAprovacao() {
        List<StatusOS> proximos = StatusOS.AGUARDANDO_APROVACAO.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.EM_EXECUCAO));
    }

    @Test
    void deveRetornarProximoStatusParaEmExecucao() {
        List<StatusOS> proximos = StatusOS.EM_EXECUCAO.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.FINALIZADA));
    }

    @Test
    void deveRetornarProximoStatusParaFinalizada() {
        List<StatusOS> proximos = StatusOS.FINALIZADA.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.ENTREGUE));
    }

    @Test
    void deveRetornarListaVaziaParaEntregue() {
        List<StatusOS> proximos = StatusOS.ENTREGUE.proximosStatusValidos();
        assertTrue(proximos.isEmpty());
    }

    @Test
    void deveValidarTransicaoValida() {
        assertTrue(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.EM_DIAGNOSTICO));
        assertTrue(StatusOS.EM_DIAGNOSTICO.podeTransicionarPara(StatusOS.AGUARDANDO_APROVACAO));
        assertTrue(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.EM_EXECUCAO));
        assertTrue(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.FINALIZADA));
        assertTrue(StatusOS.FINALIZADA.podeTransicionarPara(StatusOS.ENTREGUE));
    }

    @Test
    void deveInvalidarTransicaoInvalida() {
        assertFalse(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.FINALIZADA));
        assertFalse(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.RECEBIDA));
        assertFalse(StatusOS.ENTREGUE.podeTransicionarPara(StatusOS.EM_EXECUCAO));
    }

    @Test
    void deveRetornarDescricaoCorreta() {
        assertEquals("Recebida", StatusOS.RECEBIDA.getDescricao());
        assertEquals("Em diagnóstico", StatusOS.EM_DIAGNOSTICO.getDescricao());
        assertEquals("Aguardando aprovação", StatusOS.AGUARDANDO_APROVACAO.getDescricao());
        assertEquals("Em execução", StatusOS.EM_EXECUCAO.getDescricao());
        assertEquals("Finalizada", StatusOS.FINALIZADA.getDescricao());
        assertEquals("Entregue", StatusOS.ENTREGUE.getDescricao());
    }
}
