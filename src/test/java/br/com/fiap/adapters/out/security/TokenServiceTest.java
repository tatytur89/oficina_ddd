package br.com.fiap.adapters.out.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "segredo-de-teste-123456");
    }

    @Test
    void deveGerarEValidarTokenComSucesso() {
        String token = tokenService.gerarToken("admin");

        assertNotNull(token);
        assertFalse(token.isBlank());

        String subject = tokenService.validarToken(token);

        assertEquals("admin", subject);
    }

    @Test
    void deveRetornarVazioParaTokenInvalido() {
        String subject = tokenService.validarToken("token-invalido-qualquer");

        assertEquals("", subject);
    }

    @Test
    void deveRetornarVazioParaTokenAssinadoComOutroSegredo() {
        String token = tokenService.gerarToken("admin");

        ReflectionTestUtils.setField(tokenService, "secret", "outro-segredo-diferente");

        assertEquals("", tokenService.validarToken(token));
    }
}
