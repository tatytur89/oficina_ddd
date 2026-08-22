package br.com.fiap.adapters.out.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDataSeederTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AdminDataSeeder(usuarioRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "defaultUser", "admin");
        ReflectionTestUtils.setField(seeder, "defaultPassword", "admin123");
    }

    @Test
    void deveCriarAdminQuandoNaoExistir() throws Exception {
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("hash-gerado");

        seeder.run();

        verify(usuarioRepository, times(1)).salvar(any(Usuario.class));
    }

    @Test
    void naoDeveCriarAdminQuandoJaExistir() throws Exception {
        when(usuarioRepository.buscarPorUsername("admin"))
                .thenReturn(Optional.of(new Usuario(1L, "admin", "hash", "ROLE_ADMIN")));

        seeder.run();

        verify(usuarioRepository, never()).salvar(any(Usuario.class));
    }
}
