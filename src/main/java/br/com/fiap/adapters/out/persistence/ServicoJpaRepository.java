package br.com.fiap.adapters.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, Long> {
    Optional<ServicoJpaEntity> findByNome(String nome);
}
