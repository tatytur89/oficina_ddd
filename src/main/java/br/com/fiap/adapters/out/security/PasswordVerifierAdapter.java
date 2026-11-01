package br.com.fiap.adapters.out.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.fiap.ports.out.PasswordVerifierPort;

@Component
public class PasswordVerifierAdapter implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder;

    public PasswordVerifierAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean verificar(String senha, String hash) {
        return passwordEncoder.matches(senha, hash);
    }
}
