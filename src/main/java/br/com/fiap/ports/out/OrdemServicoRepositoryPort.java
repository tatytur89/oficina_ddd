package br.com.fiap.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;

public interface OrdemServicoRepositoryPort {
    OrdemServico salvar(OrdemServico os);
    Optional<OrdemServico> buscarPorId(Long id);
    List<OrdemServico> buscarTodas();
    List<OrdemServico> buscarPorClienteId(Long clienteId);
    List<OrdemServico> buscarPorVeiculoId(Long veiculoId);
    List<OrdemServico> buscarPorPecaIdEStatus(Long pecaId, List<StatusOS> statusList);
    List<OrdemServico> buscarPorStatus(StatusOS status);
    List<OrdemServico> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
}
