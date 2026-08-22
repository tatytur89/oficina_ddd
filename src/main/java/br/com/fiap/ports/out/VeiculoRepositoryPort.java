package br.com.fiap.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Veiculo;

public interface VeiculoRepositoryPort {
    Veiculo salvar(Veiculo veiculo);
    List<Veiculo> buscarTodos();
    List<Veiculo> buscarPorClienteId(Long clienteId);
    Optional<Veiculo> buscarPorPlaca(String placa);
    Optional<Veiculo> buscarPorId(Long id);
    void excluirPorId(Long id);
}
