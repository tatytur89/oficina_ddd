package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(OrdemServicoPersistenceAdapter.class)
class OrdemServicoPersistenceAdapterTest {

    @Autowired
    private OrdemServicoPersistenceAdapter osPersistenceAdapter;

    private OrdemServico novaOS(Long clienteId, Long veiculoId, StatusOS status, LocalDateTime dataAbertura) {
        return new OrdemServico(null, clienteId, veiculoId, status, dataAbertura, null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null, "chave-teste", null, null);
    }

    @Test
    @DisplayName("Deve salvar uma OS e retorná-la com ID gerado")
    void deveSalvarOS() {
        OrdemServico salva = osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));

        assertNotNull(salva.getId());
        assertEquals(StatusOS.RECEBIDA, salva.getStatus());
    }

    @Test
    @DisplayName("Deve permitir adicionar serviço e peça a uma OS recarregada do banco")
    void devePermitirAdicionarItensAposRecarregarDoBanco() {
        OrdemServico salva = osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.EM_DIAGNOSTICO, LocalDateTime.now()));

        OrdemServico recarregada = osPersistenceAdapter.buscarPorId(salva.getId()).orElseThrow();

        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)),
                br.com.fiap.domain.valueobjects.TipoServico.MANUTENCAO, 60);
        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL001", new Preco(BigDecimal.valueOf(45.90)), 50, 10);

        assertDoesNotThrow(() -> recarregada.adicionarServico(servico, 1));
        assertDoesNotThrow(() -> recarregada.adicionarPeca(peca, 1));
        assertEquals(1, recarregada.getServicos().size());
        assertEquals(1, recarregada.getPecas().size());
    }

    @Test
    @DisplayName("Deve buscar OS por ID")
    void deveBuscarPorId() {
        OrdemServico salva = osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));

        Optional<OrdemServico> resultado = osPersistenceAdapter.buscarPorId(salva.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    @DisplayName("Deve buscar todas as OS cadastradas")
    void deveBuscarTodas() {
        osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));
        osPersistenceAdapter.salvar(novaOS(2L, 2L, StatusOS.RECEBIDA, LocalDateTime.now()));

        assertEquals(2, osPersistenceAdapter.buscarTodas().size());
    }

    @Test
    @DisplayName("Deve buscar OS por cliente")
    void deveBuscarPorClienteId() {
        osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));
        osPersistenceAdapter.salvar(novaOS(2L, 2L, StatusOS.RECEBIDA, LocalDateTime.now()));

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorClienteId(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar OS por veículo")
    void deveBuscarPorVeiculoId() {
        osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));
        osPersistenceAdapter.salvar(novaOS(2L, 2L, StatusOS.RECEBIDA, LocalDateTime.now()));

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorVeiculoId(2L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar OS por status")
    void deveBuscarPorStatus() {
        osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now()));
        osPersistenceAdapter.salvar(novaOS(2L, 2L, StatusOS.EM_DIAGNOSTICO, LocalDateTime.now()));

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorStatus(StatusOS.EM_DIAGNOSTICO);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar OS por período")
    void deveBuscarPorPeriodo() {
        osPersistenceAdapter.salvar(novaOS(1L, 1L, StatusOS.RECEBIDA, LocalDateTime.of(2026, 1, 15, 10, 0)));
        osPersistenceAdapter.salvar(novaOS(2L, 2L, StatusOS.RECEBIDA, LocalDateTime.of(2026, 3, 15, 10, 0)));

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorPeriodo(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar OS por peça e status pendente de execução")
    void deveBuscarPorPecaIdEStatus() {
        PecaOS pecaOS = new PecaOS(1L, "Filtro de Óleo", "FIL001", 2, new Preco(BigDecimal.valueOf(45.90)), new Preco(BigDecimal.valueOf(91.80)));
        OrdemServico osComPeca = new OrdemServico(null, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.valueOf(91.80)), new Preco(BigDecimal.valueOf(91.80)), null, List.of(pecaOS), "chave-teste", null, null);
        osPersistenceAdapter.salvar(osComPeca);

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorPecaIdEStatus(1L,
                List.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO));

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Não deve encontrar OS por peça quando status não está na lista pendente")
    void naoDeveEncontrarPorPecaIdQuandoStatusNaoBate() {
        PecaOS pecaOS = new PecaOS(1L, "Filtro de Óleo", "FIL001", 2, new Preco(BigDecimal.valueOf(45.90)), new Preco(BigDecimal.valueOf(91.80)));
        OrdemServico osComPeca = new OrdemServico(null, 1L, 1L, StatusOS.ENTREGUE, LocalDateTime.now(), null, LocalDateTime.now(), "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.valueOf(91.80)), new Preco(BigDecimal.valueOf(91.80)), null, List.of(pecaOS), "chave-teste", null, null);
        osPersistenceAdapter.salvar(osComPeca);

        List<OrdemServico> resultado = osPersistenceAdapter.buscarPorPecaIdEStatus(1L,
                List.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO));

        assertTrue(resultado.isEmpty());
    }
}
