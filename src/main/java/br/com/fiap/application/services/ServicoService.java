package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.ports.in.ServicoUseCase;
import br.com.fiap.ports.out.ServicoRepositoryPort;

@Service
public class ServicoService implements ServicoUseCase {

    private final ServicoRepositoryPort servicoRepositoryPort;

    public ServicoService(ServicoRepositoryPort servicoRepositoryPort) {
        this.servicoRepositoryPort = servicoRepositoryPort;
    }

    @Override
    public Servico cadastrarServico(Servico servico) {
        Optional<Servico> existente = servicoRepositoryPort.buscarPorNome(servico.getNome());
        if (existente.isPresent()) {
            throw new ResourceAlreadyExistsException("Serviço com nome informado já existe.");
        }
        return servicoRepositoryPort.salvar(servico);
    }

    @Override
    public List<Servico> listarTodos() {
        return servicoRepositoryPort.buscarTodos();
    }

    @Override
    public Servico buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do serviço não pode ser nulo.");
        }
        return servicoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Override
    public Servico atualizarServico(Servico servico) {
        if (servico == null || servico.getId() == null) {
            throw new IllegalArgumentException("O serviço e o ID são obrigatórios");
        }

        Servico servicoExistente = servicoRepositoryPort.buscarPorId(servico.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + servico.getId()));

        Optional<Servico> servicoComMesmoNome = servicoRepositoryPort.buscarPorNome(servico.getNome());

        if (servicoComMesmoNome.isPresent() && !servicoComMesmoNome.get().getId().equals(servicoExistente.getId())) {
            throw new ResourceAlreadyExistsException("Serviço com nome informado já existe.");
        }

        Servico servicoAtualizado = new Servico(
                servicoExistente.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getTipo(),
                servico.getTempoEstimadoMinutos()
        );

        return servicoRepositoryPort.salvar(servicoAtualizado);
    }

    @Override
    public void excluirServico(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do serviço é obrigatório");
        }

        servicoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));

        servicoRepositoryPort.excluirPorId(id);
    }
}
