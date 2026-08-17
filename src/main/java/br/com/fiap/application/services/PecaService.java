package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.out.PecaRepositoryPort;

@Service
public class PecaService implements PecaUseCase {

    private final PecaRepositoryPort pecaRepositoryPort;

    public PecaService(PecaRepositoryPort pecaRepositoryPort) {
        this.pecaRepositoryPort = pecaRepositoryPort;
    }

    @Override
    public Peca cadastrarPeca(Peca peca) {
        Optional<Peca> existente = pecaRepositoryPort.buscarPorCodigo(peca.getCodigo());
        if (existente.isPresent()) {
            throw new ResourceAlreadyExistsException("Peça com código informado já existe.");
        }
        return pecaRepositoryPort.salvar(peca);
    }

    @Override
    public List<Peca> listarTodas() {
        return pecaRepositoryPort.buscarTodas();
    }

    @Override
    public Optional<Peca> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da peça não pode ser nulo.");
        }
        return pecaRepositoryPort.buscarPorId(id);
    }

    @Override
    public Optional<Peca> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código para busca não pode ser nulo ou vazio.");
        }
        return pecaRepositoryPort.buscarPorCodigo(codigo);
    }

    @Override
    @Transactional
    public void baixarEstoque(Long pecaId, int quantidade) {
        Peca peca = pecaRepositoryPort.buscarPorId(pecaId)
            .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada com ID: " + pecaId));
        
        peca.baixarEstoque(quantidade);
        pecaRepositoryPort.salvar(peca);
    }

    @Override
    @Transactional
    public void reporEstoque(Long pecaId, int quantidade) {
        Peca peca = pecaRepositoryPort.buscarPorId(pecaId)
            .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada com ID: " + pecaId));
        
        peca.reporEstoque(quantidade);
        pecaRepositoryPort.salvar(peca);
    }

    @Override
    public List<Peca> listarEstoqueBaixo() {
        return pecaRepositoryPort.buscarEstoqueBaixo(0);
    }
}
