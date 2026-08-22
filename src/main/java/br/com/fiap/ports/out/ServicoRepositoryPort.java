package br.com.fiap.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Servico;

public interface ServicoRepositoryPort {
    Servico salvar(Servico servico);
    List<Servico> buscarTodos();
    Optional<Servico> buscarPorId(Long id);
    Optional<Servico> buscarPorNome(String nome);
    void excluirPorId(Long id);
}
