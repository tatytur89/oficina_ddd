package br.com.fiap.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Cliente;

public interface ClienteRepositoryPort {
    Cliente salvar(Cliente cliente);
    List<Cliente> buscarTodos();
    Optional<Cliente> buscarPorDocumento(String documento);
    Optional<Cliente> buscarPorId(Long id);
    void excluirPorId(Long id);
}
