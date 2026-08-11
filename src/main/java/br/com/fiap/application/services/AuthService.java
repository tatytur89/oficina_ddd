package br.com.fiap.application.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.fiap.adapters.out.security.TokenService;
import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.in.AuthUseCase;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

@Service
public class AuthService implements AuthUseCase {

private final TokenService tokenService;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(TokenService tokenService, 
                       UsuarioRepositoryPort usuarioRepository, 
                       PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String autenticar(String username, String senha) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username)
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        // O BCryptPasswordEncoder valida a senha limpa enviada pelo usuário contra o hash armazenado no banco
        if (passwordEncoder.matches(senha, usuario.getPassword())) {
            return tokenService.gerarToken(usuario.getUsername());
        }

        throw new RuntimeException("Credenciais inválidas");
    }
}