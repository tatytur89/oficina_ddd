package br.com.fiap.adapters.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarComRoleQuandoTokenEUsuarioValidos() throws Exception {
        SecurityFilter filter = new SecurityFilter(tokenService, usuarioRepositoryPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(tokenService.validarToken("token-valido")).thenReturn("admin");
        when(usuarioRepositoryPort.buscarPorUsername("admin"))
                .thenReturn(Optional.of(new Usuario(1L, "admin", "hash", "ROLE_ADMIN")));

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void deveAutenticarSemAuthoritiesQuandoUsuarioNaoEncontrado() throws Exception {
        SecurityFilter filter = new SecurityFilter(tokenService, usuarioRepositoryPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(tokenService.validarToken("token-valido")).thenReturn("admin");
        when(usuarioRepositoryPort.buscarPorUsername("admin")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test
    void naoDeveAutenticarQuandoTokenForInvalido() throws Exception {
        SecurityFilter filter = new SecurityFilter(tokenService, usuarioRepositoryPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(tokenService.validarToken("token-invalido")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoNaoHouverHeaderAuthorization() throws Exception {
        SecurityFilter filter = new SecurityFilter(tokenService, usuarioRepositoryPort);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validarToken(any());
    }

    @Test
    void naoDeveAutenticarQuandoHeaderNaoComecaComBearer() throws Exception {
        SecurityFilter filter = new SecurityFilter(tokenService, usuarioRepositoryPort);
        when(request.getHeader("Authorization")).thenReturn("Basic algumacoisa");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validarToken(any());
    }
}
