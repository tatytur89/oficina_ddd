package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

@Repository
public class VeiculoPersistenceAdapter implements VeiculoRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(VeiculoPersistenceAdapter.class);

    private final VeiculoJpaRepository jpaRepository;

    public VeiculoPersistenceAdapter(VeiculoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        VeiculoJpaEntity entity = new VeiculoJpaEntity(
            veiculo.getId(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno(),
            veiculo.getPlaca(),
            veiculo.getClienteId()
        );

        VeiculoJpaEntity savedEntity = jpaRepository.save(entity);

        return new Veiculo(
            savedEntity.getId(),
            savedEntity.getMarca(),
            savedEntity.getModelo(),
            savedEntity.getAno(),
            savedEntity.getPlaca(),
            savedEntity.getClienteId()
        );
    }

    @Override
    public List<Veiculo> buscarTodos() {
        return jpaRepository.findAll().stream()
                .map(this::mapearParaDominioSeguro)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<Veiculo> buscarPorClienteId(Long clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream()
                .map(this::mapearParaDominioSeguro)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Veiculo> mapearParaDominioSeguro(VeiculoJpaEntity entity) {
        try {
            return Optional.of(mapearParaDominio(entity));
        } catch (IllegalArgumentException ex) {
            log.warn("Veículo com ID {} possui dados inválidos e foi ignorado na listagem: {}", entity.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return jpaRepository.findByPlaca(placa)
                .map(this::mapearParaDominio);
    }

    @Override
    public Optional<Veiculo> buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapearParaDominio);
    }

    @Override
    public void excluirPorId(Long id) {
        jpaRepository.deleteById(id);
    }

    private Veiculo mapearParaDominio(VeiculoJpaEntity entity) {
        return new Veiculo(
            entity.getId(),
            entity.getMarca(),
            entity.getModelo(),
            entity.getAno(),
            entity.getPlaca(),
            entity.getClienteId()
        );
    }
}
