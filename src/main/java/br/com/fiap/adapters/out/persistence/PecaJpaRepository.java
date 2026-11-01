package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PecaJpaEntity p WHERE p.id = :id")
    Optional<PecaJpaEntity> buscarPorIdParaAtualizacao(@Param("id") Long id);

    Optional<PecaJpaEntity> findByCodigo(String codigo);
    
    @Query("SELECT p FROM PecaJpaEntity p WHERE p.quantidadeEstoque <= :estoqueMinimo")
    List<PecaJpaEntity> findEstoqueBaixo(@Param("estoqueMinimo") int estoqueMinimo);
}
