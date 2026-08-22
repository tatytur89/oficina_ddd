package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.out.ServicoRepositoryPort;

@Repository
public class ServicoPersistenceAdapter implements ServicoRepositoryPort {

    private final ServicoJpaRepository jpaRepository;

    public ServicoPersistenceAdapter(ServicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Servico salvar(Servico servico) {
        ServicoJpaEntity entity = new ServicoJpaEntity(
            servico.getId(),
            servico.getNome(),
            servico.getDescricao(),
            servico.getPreco().getValor(),
            servico.getTipo().name(),
            servico.getTempoEstimadoMinutos()
        );

        ServicoJpaEntity savedEntity = jpaRepository.save(entity);

        return new Servico(
            savedEntity.getId(),
            savedEntity.getNome(),
            savedEntity.getDescricao(),
            new Preco(savedEntity.getPreco()),
            TipoServico.valueOf(savedEntity.getTipo()),
            savedEntity.getTempoEstimadoMinutos()
        );
    }

    @Override
    public List<Servico> buscarTodos() {
        return jpaRepository.findAll().stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public Optional<Servico> buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapearParaDominio);
    }

    @Override
    public Optional<Servico> buscarPorNome(String nome) {
        return jpaRepository.findByNome(nome)
                .map(this::mapearParaDominio);
    }

    @Override
    public void excluirPorId(Long id) {
        jpaRepository.deleteById(id);
    }

    private Servico mapearParaDominio(ServicoJpaEntity entity) {
        return new Servico(
            entity.getId(),
            entity.getNome(),
            entity.getDescricao(),
            new Preco(entity.getPreco()),
            TipoServico.valueOf(entity.getTipo()),
            entity.getTempoEstimadoMinutos()
        );
    }
}
