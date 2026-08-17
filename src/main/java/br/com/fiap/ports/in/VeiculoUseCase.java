package br.com.fiap.ports.in;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Veiculo;

public interface VeiculoUseCase {
    Veiculo cadastrarVeiculo(Veiculo veiculo);
    List<Veiculo> listarTodos();
    List<Veiculo> listarPorCliente(Long clienteId);
    Optional<Veiculo> buscarPorPlaca(String placa);
}
