package br.com.fiap.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long>{
    Optional<UsuarioJpaEntity> findByUsername(String username);
}
