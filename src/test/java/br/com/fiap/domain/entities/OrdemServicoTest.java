package br.com.fiap.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;

class OrdemServicoTest {

    @Test
    void deveCriarOrdemServicoComSucesso() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), LocalDateTime.now().plusDays(7),
            null, "Teste observação",
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        assertEquals(1L, os.getId());
        assertEquals(1L, os.getClienteId());
        assertEquals(1L, os.getVeiculoId());
        assertEquals(StatusOS.RECEBIDA, os.getStatus());
        assertNotNull(os.getDataAbertura());
        assertEquals("Teste observação", os.getObservacoes());
    }

    @Test
    void deveAdicionarServico() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);

        os.adicionarServico(servico, 1);

        assertEquals(1, os.getServicos().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(os.getValorServicos().getValor()));
        assertEquals(0, new BigDecimal("150.00").compareTo(os.getValorTotal().getValor()));
    }

    @Test
    void deveAdicionarPeca() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição",
            "FIL001", new Preco(45.90), 50, 10);

        os.adicionarPeca(peca, 2);

        assertEquals(1, os.getPecas().size());
        assertEquals(0, new BigDecimal("91.80").compareTo(os.getValorPecas().getValor()));
        assertEquals(0, new BigDecimal("91.80").compareTo(os.getValorTotal().getValor()));
    }

    @Test
    void naoDeveAdicionarServicoForaDeRecebidaOuEmDiagnostico() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.AGUARDANDO_APROVACAO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);

        assertThrows(IllegalStateException.class, () -> os.adicionarServico(servico, 1));
    }

    @Test
    void deveEnviarOrcamento() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.EM_DIAGNOSTICO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);
        os.adicionarServico(servico, 1);

        os.enviarOrcamento();

        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    void deveLancarExcecaoAoEnviarOrcamentoSemServicos() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.EM_DIAGNOSTICO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        assertThrows(IllegalStateException.class, () -> os.enviarOrcamento());
    }

    @Test
    void deveLancarExcecaoAoEnviarOrcamentoForaDeEmDiagnostico() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        assertThrows(IllegalStateException.class, () -> os.enviarOrcamento());
    }

    @Test
    void deveTransicionarDeAguardandoAprovacaoParaEmExecucao() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.AGUARDANDO_APROVACAO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        os.transicionarPara(StatusOS.EM_EXECUCAO);

        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
    }

    @Test
    void deveFinalizarOSERegistrarDataConclusao() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.EM_EXECUCAO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        os.transicionarPara(StatusOS.FINALIZADA);

        assertEquals(StatusOS.FINALIZADA, os.getStatus());
        assertNotNull(os.getDataConclusao());
    }

    @Test
    void deveEntregarOS() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.FINALIZADA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        os.transicionarPara(StatusOS.ENTREGUE);

        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    void deveLancarExcecaoParaTransicaoInvalida() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.ENTREGUE,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        assertThrows(IllegalStateException.class, () -> os.transicionarPara(StatusOS.EM_EXECUCAO));
    }

    @Test
    void deveRecalcularValoresAoAdicionarServicoEPeca() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);
        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição",
            "FIL001", new Preco(45.90), 50, 10);

        os.adicionarServico(servico, 1);
        os.adicionarPeca(peca, 2);

        assertEquals(0, new BigDecimal("150.00").compareTo(os.getValorServicos().getValor()));
        assertEquals(0, new BigDecimal("91.80").compareTo(os.getValorPecas().getValor()));
        assertEquals(0, new BigDecimal("241.80").compareTo(os.getValorTotal().getValor()));
    }
}
