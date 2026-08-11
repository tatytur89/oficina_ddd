package br.com.fiap.adapters.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

@Repository
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort{
    private final UsuarioJpaRepository repository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return repository.findByUsername(username)
                .map(entity -> new Usuario(
                        entity.getId(), 
                        entity.getUsername(), 
                        entity.getPassword(), 
                        entity.getRole()
                ));
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getRole()
        );
        UsuarioJpaEntity saved = repository.save(entity);
        return new Usuario(saved.getId(), saved.getUsername(), saved.getPassword(), saved.getRole());
    }

}
