package br.com.fiap.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT os FROM OrdemServicoJpaEntity os WHERE os.id = :id")
    Optional<OrdemServicoJpaEntity> findByIdParaAtualizacao(@Param("id") Long id);

    List<OrdemServicoJpaEntity> findByClienteId(Long clienteId);
    List<OrdemServicoJpaEntity> findByVeiculoId(Long veiculoId);
    List<OrdemServicoJpaEntity> findByPecas_PecaIdAndStatusIn(Long pecaId, List<String> status);
    List<OrdemServicoJpaEntity> findByStatus(String status);
    
    @Query("SELECT os FROM OrdemServicoJpaEntity os WHERE os.dataAbertura BETWEEN :inicio AND :fim")
    List<OrdemServicoJpaEntity> findByPeriodo(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fim") LocalDateTime fim
    );
}
