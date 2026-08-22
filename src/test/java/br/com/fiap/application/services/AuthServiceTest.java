package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.fiap.adapters.out.security.TokenService;
import br.com.fiap.application.exceptions.InvalidCredentialsException;
import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar o token")
    void deveAutenticarComSucesso() {
        Usuario usuario = new Usuario(1L, "admin", "hash-da-senha", "ROLE_ADMIN");
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", "hash-da-senha")).thenReturn(true);
        when(tokenService.gerarToken("admin")).thenReturn("token-jwt-gerado");

        String resultado = authService.autenticar("admin", "admin123");

        assertEquals("token-jwt-gerado", resultado);
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException para usuário inexistente")
    void deveLancarInvalidCredentialsExceptionParaUsuarioInexistente() {
        when(usuarioRepository.buscarPorUsername("desconhecido")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.autenticar("desconhecido", "qualquer"));
        verify(tokenService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException para senha incorreta")
    void deveLancarInvalidCredentialsExceptionParaSenhaIncorreta() {
        Usuario usuario = new Usuario(1L, "admin", "hash-da-senha", "ROLE_ADMIN");
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hash-da-senha")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.autenticar("admin", "senhaErrada"));
        verify(tokenService, never()).gerarToken(any());
    }
}
