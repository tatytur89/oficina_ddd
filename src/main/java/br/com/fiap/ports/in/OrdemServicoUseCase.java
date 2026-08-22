package br.com.fiap.ports.in;

import java.time.LocalDateTime;
import java.util.List;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;

public interface OrdemServicoUseCase {
    OrdemServico criarOS(OrdemServico os, List<ItemQuantidade> servicos, List<ItemQuantidade> pecas);
    OrdemServico buscarPorId(Long id);
    List<OrdemServico> listarTodas();
    List<OrdemServico> listarPorCliente(Long clienteId);
    List<OrdemServico> listarPorStatus(StatusOS status);
    List<OrdemServico> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
    OrdemServico atualizarStatus(Long id, StatusOS novoStatus);
    OrdemServico enviarOrcamento(Long id);
}
