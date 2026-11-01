package br.com.fiap.application.services;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.*;
import br.com.fiap.domain.valueobjects.*;
import br.com.fiap.ports.in.*;
import br.com.fiap.ports.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AberturaOrdemServicoTest {
    @Mock OrdemServicoRepositoryPort osRepositoryPort;
    @Mock ClienteRepositoryPort clienteRepositoryPort;
    @Mock VeiculoRepositoryPort veiculoRepositoryPort;
    @Mock ServicoUseCase servicoUseCase;
    @Mock PecaUseCase pecaUseCase;
    @Mock StatusOsNotificationPort notifications;
    @InjectMocks OrdemServicoService service;

    private final Cliente cliente = new Cliente(1L, "Cliente", "12345678909", "cliente@example.com", "11999999999");
    private final Veiculo veiculo = new Veiculo(2L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
    private final Servico servico = new Servico(3L, "Troca de oleo", "Manutencao", new Preco(new BigDecimal("150.25")), TipoServico.MANUTENCAO, 60);
    private final Peca peca = new Peca(4L, "Filtro", "Filtro de oleo", "FIL001", new Preco(new BigDecimal("45.90")), 10, 2);

    private void clienteEVeiculoExistem() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(veiculoRepositoryPort.buscarPorId(2L)).thenReturn(Optional.of(veiculo));
    }

    @Test
    void abreRecebidaComItensTotaisEIdentificacaoSemConsumirEstoque() {
        clienteEVeiculoExistem();
        when(servicoUseCase.buscarPorId(3L)).thenReturn(servico);
        when(pecaUseCase.buscarPorId(4L)).thenReturn(peca);
        when(osRepositoryPort.salvar(any())).thenAnswer(inv -> {
            OrdemServico os = inv.getArgument(0);
            return new OrdemServico(42L, os.getClienteId(), os.getVeiculoId(), os.getStatus(), os.getDataAbertura(),
                    os.getDataPrevistaEntrega(), os.getDataConclusao(), os.getObservacoes(), os.getValorServicos(),
                    os.getValorPecas(), os.getValorTotal(), os.getServicos(), os.getPecas(), os.getChaveAcesso(), null, null);
        });

        OrdemServico criada = service.criarOS(new CriarOrdemServicoCommand(1L, 2L, "Freio", List.of(new ItemDiagnostico(3L, 2)), List.of(new ItemDiagnostico(4L, 3))));

        assertEquals(42L, criada.getId());
        assertEquals(StatusOS.RECEBIDA, criada.getStatus());
        assertEquals(1L, criada.getClienteId());
        assertEquals(2L, criada.getVeiculoId());
        assertEquals("Freio", criada.getObservacoes());
        assertNotNull(criada.getDataAbertura());
        assertNull(criada.getDataConclusao());
        assertDoesNotThrow(() -> UUID.fromString(criada.getChaveAcesso()));
        assertEquals(new Preco(new BigDecimal("300.50")), criada.getValorServicos());
        assertEquals(new Preco(new BigDecimal("137.70")), criada.getValorPecas());
        assertEquals(new Preco(new BigDecimal("438.20")), criada.getValorTotal());
        assertEquals(2, criada.getServicos().getFirst().getQuantidade());
        assertEquals(3, criada.getPecas().getFirst().getQuantidade());
        assertEquals("Filtro", criada.getPecas().getFirst().getNomePeca());
        assertEquals(10, peca.getQuantidadeEstoque());
        verify(pecaUseCase, never()).baixarEstoque(anyLong(), anyInt());
        verify(osRepositoryPort).salvar(any());
    }

    @Test
    void permiteAberturaSemItensPorCompatibilidade() {
        clienteEVeiculoExistem();
        when(osRepositoryPort.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        OrdemServico criada = service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, null, null));
        assertEquals(StatusOS.RECEBIDA, criada.getStatus());
        assertTrue(criada.getServicos().isEmpty());
        assertTrue(criada.getPecas().isEmpty());
        assertEquals(new Preco(BigDecimal.ZERO), criada.getValorTotal());
        verifyNoInteractions(servicoUseCase, pecaUseCase);
    }

    @Test
    void rejeitaVeiculoDeOutroCliente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(veiculoRepositoryPort.buscarPorId(2L)).thenReturn(Optional.of(new Veiculo(2L, "Toyota", "Corolla", 2022, "ABC1D23", 99L)));
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, List.of(), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @Test
    void rejeitaServicoInexistenteSemSalvarOS() {
        clienteEVeiculoExistem();
        when(servicoUseCase.buscarPorId(3L)).thenThrow(new ResourceNotFoundException("Servico inexistente"));
        assertThrows(ResourceNotFoundException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, List.of(new ItemDiagnostico(3L, 1)), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @Test
    void rejeitaPecaInexistenteSemSalvarOS() {
        clienteEVeiculoExistem();
        when(pecaUseCase.buscarPorId(4L)).thenThrow(new ResourceNotFoundException("Peca inexistente"));
        assertThrows(ResourceNotFoundException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, List.of(), List.of(new ItemDiagnostico(4L, 1)))));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejeitaIdDeClienteInvalido(long id) {
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(id, 2L, null, List.of(), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejeitaIdDeVeiculoInvalido(long id) {
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, id, null, List.of(), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @Test
    void rejeitaIdentificadoresObrigatoriosNulos() {
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(null, 2L, null, List.of(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, null, null, List.of(), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeDeServicoInvalida(int quantidade) {
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, List.of(new ItemDiagnostico(3L, quantidade)), List.of())));
        verify(osRepositoryPort, never()).salvar(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejeitaQuantidadeDePecaInvalida(int quantidade) {
        assertThrows(IllegalArgumentException.class, () -> service.criarOS(new CriarOrdemServicoCommand(1L, 2L, null, List.of(), List.of(new ItemDiagnostico(4L, quantidade)))));
        verify(osRepositoryPort, never()).salvar(any());
    }
}
