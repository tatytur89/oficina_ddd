package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, Long> {
    Optional<VeiculoJpaEntity> findByPlaca(String placa);
    List<VeiculoJpaEntity> findByClienteId(Long clienteId);
}
