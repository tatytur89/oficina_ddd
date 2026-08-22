package br.com.fiap.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;

class OrdemServicoTest {

    private OrdemServico osComStatus(StatusOS status) {
        return new OrdemServico(
            1L, 1L, 1L, status,
            LocalDateTime.now(), null, null, null,
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null, null, null, null
        );
    }

    @Test
    void deveCriarOrdemServicoComSucesso() {
        OrdemServico os = new OrdemServico(
            1L, 1L, 1L, StatusOS.RECEBIDA,
            LocalDateTime.now(), LocalDateTime.now().plusDays(7),
            null, "Teste observação",
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null, null, null, null
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
        OrdemServico os = osComStatus(StatusOS.EM_DIAGNOSTICO);

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);

        os.adicionarServico(servico, 1);

        assertEquals(1, os.getServicos().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(os.getValorServicos().getValor()));
        assertEquals(0, new BigDecimal("150.00").compareTo(os.getValorTotal().getValor()));
    }

    @Test
    void deveAdicionarPeca() {
        OrdemServico os = osComStatus(StatusOS.EM_DIAGNOSTICO);

        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição",
            "FIL001", new Preco(45.90), 50, 10);

        os.adicionarPeca(peca, 2);

        assertEquals(1, os.getPecas().size());
        assertEquals(0, new BigDecimal("91.80").compareTo(os.getValorPecas().getValor()));
        assertEquals(0, new BigDecimal("91.80").compareTo(os.getValorTotal().getValor()));
    }

    @Test
    void naoDeveAdicionarServicoForaDeEmDiagnostico() {
        OrdemServico os = osComStatus(StatusOS.AGUARDANDO_APROVACAO);

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);

        assertThrows(IllegalStateException.class, () -> os.adicionarServico(servico, 1));
    }

    @Test
    void naoDeveAdicionarServicoEmRecebida() {
        OrdemServico os = osComStatus(StatusOS.RECEBIDA);

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);

        assertThrows(IllegalStateException.class, () -> os.adicionarServico(servico, 1));
    }

    @Test
    void naoDeveAdicionarPecaEmRecebida() {
        OrdemServico os = osComStatus(StatusOS.RECEBIDA);

        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição",
            "FIL001", new Preco(45.90), 50, 10);

        assertThrows(IllegalStateException.class, () -> os.adicionarPeca(peca, 1));
    }

    @Test
    void deveEnviarOrcamento() {
        OrdemServico os = osComStatus(StatusOS.EM_DIAGNOSTICO);

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição",
            new Preco(150.00), br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);
        os.adicionarServico(servico, 1);

        os.enviarOrcamento();

        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    void deveLancarExcecaoAoEnviarOrcamentoSemServicos() {
        OrdemServico os = osComStatus(StatusOS.EM_DIAGNOSTICO);

        assertThrows(IllegalStateException.class, () -> os.enviarOrcamento());
    }

    @Test
    void deveLancarExcecaoAoEnviarOrcamentoForaDeEmDiagnostico() {
        OrdemServico os = osComStatus(StatusOS.RECEBIDA);

        assertThrows(IllegalStateException.class, () -> os.enviarOrcamento());
    }

    @Test
    void deveTransicionarDeAguardandoAprovacaoParaEmExecucao() {
        OrdemServico os = osComStatus(StatusOS.AGUARDANDO_APROVACAO);

        os.transicionarPara(StatusOS.EM_EXECUCAO);

        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
    }

    @Test
    void deveFinalizarOSERegistrarDataConclusao() {
        OrdemServico os = osComStatus(StatusOS.EM_EXECUCAO);

        os.transicionarPara(StatusOS.FINALIZADA);

        assertEquals(StatusOS.FINALIZADA, os.getStatus());
        assertNotNull(os.getDataConclusao());
    }

    @Test
    void deveEntregarOS() {
        OrdemServico os = osComStatus(StatusOS.FINALIZADA);

        os.transicionarPara(StatusOS.ENTREGUE);

        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    void deveLancarExcecaoParaTransicaoInvalida() {
        OrdemServico os = osComStatus(StatusOS.ENTREGUE);

        assertThrows(IllegalStateException.class, () -> os.transicionarPara(StatusOS.EM_EXECUCAO));
    }

    @Test
    void deveRecalcularValoresAoAdicionarServicoEPeca() {
        OrdemServico os = osComStatus(StatusOS.EM_DIAGNOSTICO);

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

    private OrdemServico osComChave(StatusOS status, String chaveAcesso) {
        return new OrdemServico(1L, 1L, 1L, status,
            LocalDateTime.now(), null, null, "obs",
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
            null, null, chaveAcesso, null, null
        );
    }

    @Test
    void deveValidarChaveDeAcessoCorreta() {
        OrdemServico os = osComChave(StatusOS.EM_EXECUCAO, "chave-certa");

        assertDoesNotThrow(() -> os.validarChaveAcesso("chave-certa"));
    }

    @Test
    void deveLancarExcecaoParaChaveDeAcessoIncorreta() {
        OrdemServico os = osComChave(StatusOS.EM_EXECUCAO, "chave-certa");

        assertThrows(IllegalArgumentException.class, () -> os.validarChaveAcesso("chave-errada"));
    }

    @Test
    void deveLancarExcecaoParaChaveDeAcessoNula() {
        OrdemServico os = osComChave(StatusOS.EM_EXECUCAO, "chave-certa");

        assertThrows(IllegalArgumentException.class, () -> os.validarChaveAcesso(null));
    }

    @Test
    void deveContinuarValidandoChaveDeAcessoQuandoOSEntregue() {
        OrdemServico os = osComChave(StatusOS.ENTREGUE, "chave-certa");

        assertDoesNotThrow(() -> os.validarChaveAcesso("chave-certa"));
    }

    @Test
    void deveAprovarOrcamentoComChaveValida() {
        OrdemServico os = osComChave(StatusOS.AGUARDANDO_APROVACAO, "chave-certa");

        os.aprovarOrcamento("chave-certa");

        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
    }

    @Test
    void deveLancarExcecaoAoAprovarOrcamentoComChaveInvalida() {
        OrdemServico os = osComChave(StatusOS.AGUARDANDO_APROVACAO, "chave-certa");

        assertThrows(IllegalArgumentException.class, () -> os.aprovarOrcamento("chave-errada"));
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    void deveLancarExcecaoAoAprovarOrcamentoForaDeAguardandoAprovacao() {
        OrdemServico os = osComChave(StatusOS.EM_DIAGNOSTICO, "chave-certa");

        assertThrows(IllegalStateException.class, () -> os.aprovarOrcamento("chave-certa"));
    }

    @Test
    void deveAvaliarServicoQuandoEntregue() {
        OrdemServico os = osComChave(StatusOS.ENTREGUE, "chave-certa");

        os.avaliarServico(5, "Excelente atendimento");

        assertEquals(5, os.getNotaAvaliacao());
        assertEquals("Excelente atendimento", os.getComentarioAvaliacao());
    }

    @Test
    void deveAvaliarServicoSemComentario() {
        OrdemServico os = osComChave(StatusOS.ENTREGUE, "chave-certa");

        os.avaliarServico(4, null);

        assertEquals(4, os.getNotaAvaliacao());
        assertNull(os.getComentarioAvaliacao());
    }

    @Test
    void deveLancarExcecaoAoAvaliarForaDeEntregue() {
        OrdemServico os = osComChave(StatusOS.FINALIZADA, "chave-certa");

        assertThrows(IllegalStateException.class, () -> os.avaliarServico(5, "comentário"));
    }

    @Test
    void deveLancarExcecaoAoAvaliarDuasVezes() {
        OrdemServico os = osComChave(StatusOS.ENTREGUE, "chave-certa");
        os.avaliarServico(5, "primeira avaliação");

        assertThrows(IllegalStateException.class, () -> os.avaliarServico(3, "segunda avaliação"));
    }

    @Test
    void deveLancarExcecaoParaNotaForaDoIntervalo() {
        OrdemServico os = osComChave(StatusOS.ENTREGUE, "chave-certa");

        assertThrows(IllegalArgumentException.class, () -> os.avaliarServico(0, null));
        assertThrows(IllegalArgumentException.class, () -> os.avaliarServico(6, null));
    }
}
