package br.com.fiap.application.services;

import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.in.ServicoUseCase;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;
import br.com.fiap.ports.out.StatusOsNotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaOrdemServicoTest {
    @Mock OrdemServicoRepositoryPort repository;
    @Mock ClienteRepositoryPort clientes;
    @Mock VeiculoRepositoryPort veiculos;
    @Mock ServicoUseCase servicos;
    @Mock PecaUseCase pecas;
    @Mock StatusOsNotificationPort notifications;
    @InjectMocks OrdemServicoService service;

    private final LocalDateTime abertura = LocalDateTime.of(2026, 10, 1, 10, 0);

    private OrdemServico os(Long id, StatusOS status, LocalDateTime data) {
        Preco zero = new Preco(BigDecimal.ZERO);
        LocalDateTime conclusao = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE
                ? data.plusHours(2) : null;
        return new OrdemServico(id, 1L, 2L, status, data, null, conclusao, "obs", zero, zero, zero,
                null, null, "chave", null, null);
    }

    @Test
    void priorizaExecucaoAguardandoDiagnosticoERecebidaMesmoComDatasInvertidas() {
        OrdemServico recebida = os(1L, StatusOS.RECEBIDA, abertura);
        OrdemServico diagnostico = os(2L, StatusOS.EM_DIAGNOSTICO, abertura.plusDays(1));
        OrdemServico aguardando = os(3L, StatusOS.AGUARDANDO_APROVACAO, abertura.plusDays(2));
        OrdemServico execucao = os(4L, StatusOS.EM_EXECUCAO, abertura.plusDays(3));
        when(repository.buscarTodas()).thenReturn(List.of(recebida, diagnostico, execucao, aguardando));

        assertEquals(List.of(execucao, aguardando, diagnostico, recebida), service.listarTodas());
    }

    @Test
    void ordenaAsMaisAntigasPrimeiroDentroDeCadaStatus() {
        var execucaoNova = os(1L, StatusOS.EM_EXECUCAO, abertura.plusHours(1));
        var execucaoAntiga = os(2L, StatusOS.EM_EXECUCAO, abertura);
        var diagnosticoNovo = os(3L, StatusOS.EM_DIAGNOSTICO, abertura.plusDays(1));
        var diagnosticoAntigo = os(4L, StatusOS.EM_DIAGNOSTICO, abertura.minusDays(1));
        when(repository.buscarTodas()).thenReturn(List.of(diagnosticoNovo, execucaoNova, diagnosticoAntigo, execucaoAntiga));

        assertEquals(List.of(execucaoAntiga, execucaoNova, diagnosticoAntigo, diagnosticoNovo), service.listarTodas());
    }

    @Test
    void desempataMesmaDataPeloIdentificadorCrescente() {
        var terceira = os(3L, StatusOS.RECEBIDA, abertura);
        var primeira = os(1L, StatusOS.RECEBIDA, abertura);
        var segunda = os(2L, StatusOS.RECEBIDA, abertura);
        when(repository.buscarTodas()).thenReturn(List.of(terceira, primeira, segunda));

        assertEquals(List.of(primeira, segunda, terceira), service.listarTodas());
    }

    @Test
    void colocaDatasEIdentificadoresAusentesAoFinalDoRespectivoGrupo() {
        var semData = os(2L, StatusOS.RECEBIDA, null);
        var semId = os(null, StatusOS.RECEBIDA, abertura);
        var identificada = os(1L, StatusOS.RECEBIDA, abertura);
        when(repository.buscarTodas()).thenReturn(List.of(semData, semId, identificada));

        assertEquals(List.of(identificada, semId, semData), service.listarTodas());
    }

    @Test
    void ocultaConcluidasSemAlterarRegistrosOuSalvarNoRepositorio() {
        var finalizada = os(1L, StatusOS.FINALIZADA, abertura);
        var entregue = os(2L, StatusOS.ENTREGUE, abertura);
        var recebida = os(3L, StatusOS.RECEBIDA, abertura);
        var registros = new ArrayList<>(List.of(finalizada, recebida, entregue));
        when(repository.buscarTodas()).thenReturn(registros);

        assertEquals(List.of(recebida), service.listarTodas());
        assertEquals(List.of(finalizada, recebida, entregue), registros);
        assertEquals(StatusOS.FINALIZADA, finalizada.getStatus());
        assertEquals(StatusOS.ENTREGUE, entregue.getStatus());
        verify(repository).buscarTodas();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void retornaFilaVaziaQuandoSoExistemFinalizadasEEntregues() {
        when(repository.buscarTodas()).thenReturn(List.of(os(1L, StatusOS.FINALIZADA, abertura), os(2L, StatusOS.ENTREGUE, abertura)));
        assertTrue(service.listarTodas().isEmpty());
    }

    @Test
    void retornaFilaVaziaQuandoNaoExistemOrdens() {
        when(repository.buscarTodas()).thenReturn(List.of());
        assertTrue(service.listarTodas().isEmpty());
    }

    @Test
    void preservaConsultaHistoricaPorIdDeOrdemEntregue() {
        var entregue = os(2L, StatusOS.ENTREGUE, abertura);
        when(repository.buscarPorId(2L)).thenReturn(Optional.of(entregue));
        assertSame(entregue, service.buscarPorId(2L));
    }

    @Test
    void preservaConcluidasNaConsultaPorCliente() {
        var finalizada = os(1L, StatusOS.FINALIZADA, abertura);
        var entregue = os(2L, StatusOS.ENTREGUE, abertura);
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(new Cliente(1L, "Cliente", "12345678909", "cliente@example.com", "11999999999")));
        when(repository.buscarPorClienteId(1L)).thenReturn(List.of(finalizada, entregue));
        assertEquals(List.of(finalizada, entregue), service.listarPorCliente(1L));
    }

    @Test
    void preservaConcluidasNaConsultaPorPeriodo() {
        var entregue = os(2L, StatusOS.ENTREGUE, abertura);
        var fim = abertura.plusDays(1);
        when(repository.buscarPorPeriodo(abertura, fim)).thenReturn(List.of(entregue));
        assertEquals(List.of(entregue), service.listarPorPeriodo(abertura, fim));
    }

    @Test
    void preservaConsultaExplicitaPorStatusFinalizada() {
        var finalizada = os(1L, StatusOS.FINALIZADA, abertura);
        when(repository.buscarPorStatus(StatusOS.FINALIZADA)).thenReturn(List.of(finalizada));
        assertEquals(List.of(finalizada), service.listarPorStatus(StatusOS.FINALIZADA));
    }

    @Test
    void metricaContinuaConsiderandoFinalizadasEEntreguesOcultasDaFila() {
        when(repository.buscarTodas()).thenReturn(List.of(os(1L, StatusOS.FINALIZADA, abertura), os(2L, StatusOS.ENTREGUE, abertura), os(3L, StatusOS.RECEBIDA, abertura)));
        var resultado = service.calcularTempoMedioExecucao();
        assertEquals(2, resultado.quantidadeOSConsideradas());
        assertEquals(120.0, resultado.tempoMedioMinutos());
    }
}
