package br.com.fiap.ports.out;

import java.util.Optional;

import br.com.fiap.domain.entities.Usuario;

public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorUsername (String username);
    Usuario salvar(Usuario usuario);
}
