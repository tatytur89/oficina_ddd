package br.com.fiap.ports.in;

import java.time.LocalDateTime;
import java.util.List;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;

public interface OrdemServicoUseCase {
    OrdemServico criarOS(OrdemServico os);
    OrdemServico realizarDiagnostico(Long osId, List<ItemDiagnostico> servicos, List<ItemDiagnostico> pecas, LocalDateTime dataPrevistaEntrega);
    OrdemServico adicionarServico(Long osId, Long servicoId, int quantidade);
    OrdemServico adicionarPeca(Long osId, Long pecaId, int quantidade);
    OrdemServico buscarPorId(Long id);
    List<OrdemServico> listarTodas();
    List<OrdemServico> listarPorCliente(Long clienteId);
    List<OrdemServico> listarPorStatus(StatusOS status);
    List<OrdemServico> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
    OrdemServico atualizarStatus(Long id, StatusOS novoStatus);
    OrdemServico enviarOrcamento(Long id);
    OrdemServico aprovarOrcamento(Long id, String chave);
    OrdemServico avaliarServico(Long id, String chave, int nota, String comentario);
    OrdemServico buscarParaAcompanhamento(Long id, String chave);
    TempoMedioExecucao calcularTempoMedioExecucao();
}
