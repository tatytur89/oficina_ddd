package br.com.fiap.ports.in;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Cliente;

public interface ClienteUseCase {
    Cliente cadastrarCliente(Cliente cliente);
    List<Cliente> listarTodos();
    Optional<Cliente> buscarPorDocumento(String documento);
    Cliente atualizarCliente(Cliente cliente);
    void excluirCliente(Long id);
    
}
