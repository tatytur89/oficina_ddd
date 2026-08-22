package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.ports.out.PecaRepositoryPort;

@Repository
public class PecaPersistenceAdapter implements PecaRepositoryPort {

    private final PecaJpaRepository jpaRepository;

    public PecaPersistenceAdapter(PecaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Peca salvar(Peca peca) {
        PecaJpaEntity entity = new PecaJpaEntity(
            peca.getId(),
            peca.getNome(),
            peca.getDescricao(),
            peca.getCodigo(),
            peca.getPreco().getValor(),
            peca.getQuantidadeEstoque(),
            peca.getEstoqueMinimo()
        );

        PecaJpaEntity savedEntity = jpaRepository.save(entity);

        return mapearParaDominio(savedEntity);
    }

    @Override
    public List<Peca> buscarTodas() {
        return jpaRepository.findAll().stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public Optional<Peca> buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapearParaDominio);
    }

    @Override
    public Optional<Peca> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo)
                .map(this::mapearParaDominio);
    }

    @Override
    public List<Peca> buscarEstoqueBaixo(int estoqueMinimo) {
        return jpaRepository.findEstoqueBaixo(estoqueMinimo).stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public void excluirPorId(Long id) {
        jpaRepository.deleteById(id);
    }

    private Peca mapearParaDominio(PecaJpaEntity entity) {
        return new Peca(
            entity.getId(),
            entity.getNome(),
            entity.getDescricao(),
            entity.getCodigo(),
            new Preco(entity.getPreco()),
            entity.getQuantidadeEstoque(),
            entity.getEstoqueMinimo()
        );
    }
}
