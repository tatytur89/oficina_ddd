package br.com.fiap.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class StatusOSTest {

    @Test
    void deveRetornarProximosStatusParaRecebida() {
        List<StatusOS> proximos = StatusOS.RECEBIDA.proximosStatusValidos();
        assertEquals(2, proximos.size());
        assertTrue(proximos.contains(StatusOS.EM_ANDAMENTO));
        assertTrue(proximos.contains(StatusOS.CANCELADA));
    }

    @Test
    void deveRetornarProximosStatusParaEmAndamento() {
        List<StatusOS> proximos = StatusOS.EM_ANDAMENTO.proximosStatusValidos();
        assertEquals(2, proximos.size());
        assertTrue(proximos.contains(StatusOS.AGUARDANDO_APROVACAO));
        assertTrue(proximos.contains(StatusOS.CANCELADA));
    }

    @Test
    void deveRetornarProximosStatusParaAguardandoAprovacao() {
        List<StatusOS> proximos = StatusOS.AGUARDANDO_APROVACAO.proximosStatusValidos();
        assertEquals(2, proximos.size());
        assertTrue(proximos.contains(StatusOS.APROVADA));
        assertTrue(proximos.contains(StatusOS.CANCELADA));
    }

    @Test
    void deveRetornarProximosStatusParaAprovada() {
        List<StatusOS> proximos = StatusOS.APROVADA.proximosStatusValidos();
        assertEquals(2, proximos.size());
        assertTrue(proximos.contains(StatusOS.EM_EXECUCAO));
        assertTrue(proximos.contains(StatusOS.CANCELADA));
    }

    @Test
    void deveRetornarProximosStatusParaEmExecucao() {
        List<StatusOS> proximos = StatusOS.EM_EXECUCAO.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.CONCLUIDA));
    }

    @Test
    void deveRetornarProximosStatusParaConcluida() {
        List<StatusOS> proximos = StatusOS.CONCLUIDA.proximosStatusValidos();
        assertEquals(1, proximos.size());
        assertTrue(proximos.contains(StatusOS.ENTREGUE));
    }

    @Test
    void deveRetornarListaVaziaParaEntregue() {
        List<StatusOS> proximos = StatusOS.ENTREGUE.proximosStatusValidos();
        assertTrue(proximos.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaParaCancelada() {
        List<StatusOS> proximos = StatusOS.CANCELADA.proximosStatusValidos();
        assertTrue(proximos.isEmpty());
    }

    @Test
    void deveValidarTransicaoValida() {
        assertTrue(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.EM_ANDAMENTO));
        assertTrue(StatusOS.EM_ANDAMENTO.podeTransicionarPara(StatusOS.AGUARDANDO_APROVACAO));
        assertTrue(StatusOS.AGUARDANDO_APROVACAO.podeTransicionarPara(StatusOS.APROVADA));
        assertTrue(StatusOS.APROVADA.podeTransicionarPara(StatusOS.EM_EXECUCAO));
        assertTrue(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.CONCLUIDA));
        assertTrue(StatusOS.CONCLUIDA.podeTransicionarPara(StatusOS.ENTREGUE));
    }

    @Test
    void deveInvalidarTransicaoInvalida() {
        assertFalse(StatusOS.RECEBIDA.podeTransicionarPara(StatusOS.CONCLUIDA));
        assertFalse(StatusOS.EM_EXECUCAO.podeTransicionarPara(StatusOS.RECEBIDA));
        assertFalse(StatusOS.ENTREGUE.podeTransicionarPara(StatusOS.EM_EXECUCAO));
    }

    @Test
    void deveRetornarDescricaoCorreta() {
        assertEquals("Recebida", StatusOS.RECEBIDA.getDescricao());
        assertEquals("Em Andamento", StatusOS.EM_ANDAMENTO.getDescricao());
        assertEquals("Aguardando Aprovação", StatusOS.AGUARDANDO_APROVACAO.getDescricao());
    }
}
