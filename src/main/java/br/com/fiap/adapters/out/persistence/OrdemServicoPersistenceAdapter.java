package br.com.fiap.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.ServicoOS;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;

@Repository
public class OrdemServicoPersistenceAdapter implements OrdemServicoRepositoryPort {

    private final OrdemServicoJpaRepository jpaRepository;

    public OrdemServicoPersistenceAdapter(OrdemServicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrdemServico salvar(OrdemServico os) {
        OrdemServicoJpaEntity entity = new OrdemServicoJpaEntity(
            os.getId(),
            os.getClienteId(),
            os.getVeiculoId(),
            os.getStatus().name(),
            os.getDataAbertura(),
            os.getDataPrevistaEntrega(),
            os.getDataConclusao(),
            os.getObservacoes(),
            os.getValorServicos().getValor(),
            os.getValorPecas().getValor(),
            os.getValorTotal().getValor()
        );

        List<ServicoOSJpaEntity> servicoEntities = os.getServicos().stream()
            .map(s -> new ServicoOSJpaEntity(
                null, s.getServicoId(), s.getNomeServico(), s.getQuantidade(),
                s.getPrecoUnitario().getValor(), s.getValorTotal().getValor()
            ))
            .toList();
        entity.setServicos(new ArrayList<>(servicoEntities));

        List<PecaOSJpaEntity> pecaEntities = os.getPecas().stream()
            .map(p -> new PecaOSJpaEntity(
                null, p.getPecaId(), p.getNomePeca(), p.getCodigoPeca(),
                p.getQuantidade(), p.getPrecoUnitario().getValor(), p.getValorTotal().getValor()
            ))
            .toList();
        entity.setPecas(new ArrayList<>(pecaEntities));

        OrdemServicoJpaEntity savedEntity = jpaRepository.save(entity);

        return mapearParaDominio(savedEntity);
    }

    @Override
    public Optional<OrdemServico> buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapearParaDominio);
    }

    @Override
    public List<OrdemServico> buscarTodas() {
        return jpaRepository.findAll().stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public List<OrdemServico> buscarPorClienteId(Long clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public List<OrdemServico> buscarPorVeiculoId(Long veiculoId) {
        return jpaRepository.findByVeiculoId(veiculoId).stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public List<OrdemServico> buscarPorStatus(StatusOS status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public List<OrdemServico> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jpaRepository.findByPeriodo(inicio, fim).stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    private OrdemServico mapearParaDominio(OrdemServicoJpaEntity entity) {
        List<ServicoOS> servicos = entity.getServicos().stream()
            .map(s -> new ServicoOS(
                s.getServicoId(), s.getNomeServico(), s.getQuantidade(),
                new Preco(s.getPrecoUnitario()), new Preco(s.getValorTotal())
            ))
            .toList();

        List<PecaOS> pecas = entity.getPecas().stream()
            .map(p -> new PecaOS(
                p.getPecaId(), p.getNomePeca(), p.getCodigoPeca(),
                p.getQuantidade(), new Preco(p.getPrecoUnitario()), new Preco(p.getValorTotal())
            ))
            .toList();

        return new OrdemServico(
            entity.getId(),
            entity.getClienteId(),
            entity.getVeiculoId(),
            StatusOS.valueOf(entity.getStatus()),
            entity.getDataAbertura(),
            entity.getDataPrevistaEntrega(),
            entity.getDataConclusao(),
            entity.getObservacoes(),
            new Preco(entity.getValorServicos()),
            new Preco(entity.getValorPecas()),
            new Preco(entity.getValorTotal()),
            servicos,
            pecas
        );
    }
}
