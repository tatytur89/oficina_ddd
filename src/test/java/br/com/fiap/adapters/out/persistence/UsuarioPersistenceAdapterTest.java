package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.Usuario;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(UsuarioPersistenceAdapter.class)
class UsuarioPersistenceAdapterTest {

    @Autowired
    private UsuarioPersistenceAdapter usuarioPersistenceAdapter;

    @Test
    @DisplayName("Deve salvar um usuário e retorná-lo com ID gerado")
    void deveSalvarUsuario() {
        Usuario usuario = new Usuario(null, "admin", "hash-da-senha", "ROLE_ADMIN");

        Usuario salvo = usuarioPersistenceAdapter.salvar(usuario);

        assertNotNull(salvo.getId());
        assertEquals("admin", salvo.getUsername());
        assertEquals("ROLE_ADMIN", salvo.getRole());
    }

    @Test
    @DisplayName("Deve buscar usuário por username")
    void deveBuscarPorUsername() {
        usuarioPersistenceAdapter.salvar(new Usuario(null, "admin", "hash-da-senha", "ROLE_ADMIN"));

        Optional<Usuario> resultado = usuarioPersistenceAdapter.buscarPorUsername("admin");

        assertTrue(resultado.isPresent());
        assertEquals("hash-da-senha", resultado.get().getPassword());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar username inexistente")
    void deveRetornarVazioAoBuscarUsernameInexistente() {
        Optional<Usuario> resultado = usuarioPersistenceAdapter.buscarPorUsername("desconhecido");

        assertTrue(resultado.isEmpty());
    }
}
