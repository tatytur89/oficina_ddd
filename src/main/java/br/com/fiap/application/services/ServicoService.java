package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

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
        return servicoRepositoryPort.salvar(servico);
    }

    @Override
    public List<Servico> listarTodos() {
        return servicoRepositoryPort.buscarTodos();
    }

    @Override
    public Optional<Servico> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do serviço não pode ser nulo.");
        }
        return servicoRepositoryPort.buscarPorId(id);
    }
}
