package br.com.fiap.ports.in;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Servico;

public interface ServicoUseCase {
    Servico cadastrarServico(Servico servico);
    List<Servico> listarTodos();
    Optional<Servico> buscarPorId(Long id);
}
