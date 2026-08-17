package br.com.fiap.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, Long> {
    List<OrdemServicoJpaEntity> findByClienteId(Long clienteId);
    List<OrdemServicoJpaEntity> findByStatus(String status);
    
    @Query("SELECT os FROM OrdemServicoJpaEntity os WHERE os.dataAbertura BETWEEN :inicio AND :fim")
    List<OrdemServicoJpaEntity> findByPeriodo(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fim") LocalDateTime fim
    );
}
