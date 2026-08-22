package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.VeiculoUseCase;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

@Service
public class VeiculoService implements VeiculoUseCase {

    private final VeiculoRepositoryPort veiculoRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;

    public VeiculoService(VeiculoRepositoryPort veiculoRepositoryPort, ClienteRepositoryPort clienteRepositoryPort) {
        this.veiculoRepositoryPort = veiculoRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
    }

    @Override
    public Veiculo cadastrarVeiculo(Veiculo veiculo) {
        clienteRepositoryPort.buscarPorId(veiculo.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + veiculo.getClienteId()));

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
    public Veiculo buscarPorPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("A placa para busca não pode ser nula ou vazia.");
        }
        String placaFormatada = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return veiculoRepositoryPort.buscarPorPlaca(placaFormatada)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com a placa: " + placaFormatada));
    }

    @Override
    public Veiculo atualizarVeiculo(Veiculo veiculo) {
        if (veiculo == null || veiculo.getId() == null) {
            throw new IllegalArgumentException("O veículo e o ID são obrigatórios");
        }

        Veiculo veiculoExistente = veiculoRepositoryPort.buscarPorId(veiculo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com ID: " + veiculo.getId()));

        clienteRepositoryPort.buscarPorId(veiculo.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + veiculo.getClienteId()));

        Optional<Veiculo> veiculoComMesmaPlaca = veiculoRepositoryPort.buscarPorPlaca(veiculo.getPlaca());

        if (veiculoComMesmaPlaca.isPresent() && !veiculoComMesmaPlaca.get().getId().equals(veiculoExistente.getId())) {
            throw new ResourceAlreadyExistsException("Veículo com placa informada já existe.");
        }

        Veiculo veiculoAtualizado = new Veiculo(
                veiculoExistente.getId(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getPlaca(),
                veiculo.getClienteId()
        );

        return veiculoRepositoryPort.salvar(veiculoAtualizado);
    }

    @Override
    public void excluirVeiculo(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do veículo é obrigatório");
        }

        veiculoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com ID: " + id));

        veiculoRepositoryPort.excluirPorId(id);
    }
}
