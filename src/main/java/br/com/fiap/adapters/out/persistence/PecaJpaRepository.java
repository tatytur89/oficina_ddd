package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, Long> {
    Optional<PecaJpaEntity> findByCodigo(String codigo);
    
    @Query("SELECT p FROM PecaJpaEntity p WHERE p.quantidadeEstoque <= :estoqueMinimo")
    List<PecaJpaEntity> findEstoqueBaixo(@Param("estoqueMinimo") int estoqueMinimo);
}
