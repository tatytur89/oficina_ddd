package br.com.fiap.ports.in;

import java.util.List;

import br.com.fiap.domain.entities.Servico;

public interface ServicoUseCase {
    Servico cadastrarServico(Servico servico);
    List<Servico> listarTodos();
    Servico buscarPorId(Long id);
    Servico atualizarServico(Servico servico);
    void excluirServico(Long id);
}
