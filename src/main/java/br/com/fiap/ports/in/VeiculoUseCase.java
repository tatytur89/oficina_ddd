package br.com.fiap.ports.in;

import java.util.List;

import br.com.fiap.domain.entities.Veiculo;

public interface VeiculoUseCase {
    Veiculo cadastrarVeiculo(Veiculo veiculo);
    List<Veiculo> listarTodos();
    List<Veiculo> listarPorCliente(Long clienteId);
    Veiculo buscarPorPlaca(String placa);
    Veiculo atualizarVeiculo(Veiculo veiculo);
    void excluirVeiculo(Long id);
}
