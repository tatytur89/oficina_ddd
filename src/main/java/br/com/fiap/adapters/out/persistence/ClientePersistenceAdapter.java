package br.com.fiap.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.out.ClienteRepositoryPort;

@Repository
public class ClientePersistenceAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository jpaRepository;
    public ClientePersistenceAdapter(ClienteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
       // Mapeia do Domínio para JPA
        ClienteJpaEntity entity = new ClienteJpaEntity(
            cliente.getId(),
            cliente.getNome(),
            cliente.getDocumento(),
            cliente.getEmail(),
            cliente.getTelefone()
        );

        ClienteJpaEntity savedEntity = jpaRepository.save(entity);
        
        return new Cliente(
            savedEntity.getId(),
            savedEntity.getNome(),
            savedEntity.getDocumento(),
            savedEntity.getEmail(),
            savedEntity.getTelefone()
        );
    }

@Override
    public List<Cliente> buscarTodos() {
        return jpaRepository.findAll().stream()
                .map(this::mapearParaDominio)
                .toList();
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return jpaRepository.findByDocumento(documento)
                .map(this::mapearParaDominio);
    }

    // Método auxiliar para evitar duplicação de mapeamento
    private Cliente mapearParaDominio(ClienteJpaEntity entity) {
        return new Cliente(
            entity.getId(),
            entity.getNome(),
            entity.getDocumento(),
            entity.getEmail(),
            entity.getTelefone()
        );
    }
}

