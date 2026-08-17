package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.VeiculoUseCase;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

@Service
public class VeiculoService implements VeiculoUseCase {

    private final VeiculoRepositoryPort veiculoRepositoryPort;

    public VeiculoService(VeiculoRepositoryPort veiculoRepositoryPort) {
        this.veiculoRepositoryPort = veiculoRepositoryPort;
    }

    @Override
    public Veiculo cadastrarVeiculo(Veiculo veiculo) {
        Optional<Veiculo> existente = veiculoRepositoryPort.buscarPorPlaca(veiculo.getPlaca());
        if (existente.isPresent()) {
            throw new ResourceAlreadyExistsException("Veículo com placa informada já existe.");
        }
        return veiculoRepositoryPort.salvar(veiculo);
    }

    @Override
    public List<Veiculo> listarTodos() {
        return veiculoRepositoryPort.buscarTodos();
    }

    @Override
    public List<Veiculo> listarPorCliente(Long clienteId) {
        return veiculoRepositoryPort.buscarPorClienteId(clienteId);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("A placa para busca não pode ser nula ou vazia.");
        }
        String placaFormatada = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return veiculoRepositoryPort.buscarPorPlaca(placaFormatada);
    }
}
