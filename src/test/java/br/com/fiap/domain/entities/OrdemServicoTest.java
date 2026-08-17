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
    void deveEnviarOrcamento() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.EM_ANDAMENTO,
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
            1L, 1L, 1L, StatusOS.EM_ANDAMENTO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        assertThrows(IllegalStateException.class, () -> os.enviarOrcamento());
    }

    @Test
    void deveAprovarOS() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.AGUARDANDO_APROVACAO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        os.aprovar();
        
        assertEquals(StatusOS.APROVADA, os.getStatus());
    }

    @Test
    void deveIniciarExecucao() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.APROVADA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        os.iniciarExecucao();
        
        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
    }

    @Test
    void deveConcluirOS() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.EM_EXECUCAO,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        os.concluir();
        
        assertEquals(StatusOS.CONCLUIDA, os.getStatus());
        assertNotNull(os.getDataConclusao());
    }

    @Test
    void deveEntregarOS() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.CONCLUIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        os.entregar();
        
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    void deveCancelarOS() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        os.cancelar();
        
        assertEquals(StatusOS.CANCELADA, os.getStatus());
    }

    @Test
    void deveLancarExcecaoParaTransicaoInvalida() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.ENTREGUE,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null
        );
        
        assertThrows(IllegalStateException.class, () -> os.cancelar());
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
