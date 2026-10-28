package br.com.fiap.adapters.out.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordVerifierAdapterTest {

    @Test
    void deveVerificarSenhaContraHashBCrypt() {
        var encoder = new BCryptPasswordEncoder();
        var adapter = new PasswordVerifierAdapter(encoder);
        String hash = encoder.encode("senha-correta");

        assertTrue(adapter.verificar("senha-correta", hash));
        assertFalse(adapter.verificar("senha-incorreta", hash));
    }
}
