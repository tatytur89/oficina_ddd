package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceInUseException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.PecaRepositoryPort;

@Service
public class PecaService implements PecaUseCase {

    private static final List<StatusOS> STATUS_PENDENTES_DE_EXECUCAO =
            List.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO);

    private final PecaRepositoryPort pecaRepositoryPort;
    private final OrdemServicoRepositoryPort osRepositoryPort;

    public PecaService(PecaRepositoryPort pecaRepositoryPort, OrdemServicoRepositoryPort osRepositoryPort) {
        this.pecaRepositoryPort = pecaRepositoryPort;
        this.osRepositoryPort = osRepositoryPort;
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
    public Peca buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da peça não pode ser nulo.");
        }
        return pecaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + id));
    }

    @Override
    public Peca buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código para busca não pode ser nulo ou vazio.");
        }
        return pecaRepositoryPort.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com código: " + codigo));
    }

    @Override
    @Transactional
    public void baixarEstoque(Long pecaId, int quantidade) {
        Peca peca = pecaRepositoryPort.buscarPorId(pecaId)
            .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + pecaId));

        peca.baixarEstoque(quantidade);
        pecaRepositoryPort.salvar(peca);
    }

    @Override
    @Transactional
    public void reporEstoque(Long pecaId, int quantidade) {
        Peca peca = pecaRepositoryPort.buscarPorId(pecaId)
            .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + pecaId));

        peca.reporEstoque(quantidade);
        pecaRepositoryPort.salvar(peca);
    }

    @Override
    public List<Peca> listarEstoqueBaixo() {
        return pecaRepositoryPort.buscarEstoqueBaixo(0);
    }

    @Override
    public Peca atualizarPeca(Peca peca) {
        if (peca == null || peca.getId() == null) {
            throw new IllegalArgumentException("A peça e o ID são obrigatórios");
        }

        Peca pecaExistente = pecaRepositoryPort.buscarPorId(peca.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + peca.getId()));

        Optional<Peca> pecaComMesmoCodigo = pecaRepositoryPort.buscarPorCodigo(peca.getCodigo());

        if (pecaComMesmoCodigo.isPresent() && !pecaComMesmoCodigo.get().getId().equals(pecaExistente.getId())) {
            throw new ResourceAlreadyExistsException("Peça com código informado já existe.");
        }

        Peca pecaAtualizada = new Peca(
                pecaExistente.getId(),
                peca.getNome(),
                peca.getDescricao(),
                peca.getCodigo(),
                peca.getPreco(),
                pecaExistente.getQuantidadeEstoque(),
                peca.getEstoqueMinimo()
        );

        return pecaRepositoryPort.salvar(pecaAtualizada);
    }

    @Override
    public void excluirPeca(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da peça é obrigatório");
        }

        pecaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + id));

        if (!osRepositoryPort.buscarPorPecaIdEStatus(id, STATUS_PENDENTES_DE_EXECUCAO).isEmpty()) {
            throw new ResourceInUseException("Peça está vinculada a ordens de serviço pendentes de execução e não pode ser excluída.");
        }

        pecaRepositoryPort.excluirPorId(id);
    }
}
