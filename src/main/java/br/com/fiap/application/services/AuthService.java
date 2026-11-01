package br.com.fiap.application.services;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.InvalidCredentialsException;
import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.in.AuthUseCase;
import br.com.fiap.ports.out.PasswordVerifierPort;
import br.com.fiap.ports.out.TokenGeneratorPort;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

@Service
public class AuthService implements AuthUseCase {

    private final TokenGeneratorPort tokenGenerator;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordVerifierPort passwordVerifier;

    public AuthService(TokenGeneratorPort tokenGenerator,
                       UsuarioRepositoryPort usuarioRepository,
                       PasswordVerifierPort passwordVerifier) {
        this.tokenGenerator = tokenGenerator;
        this.usuarioRepository = usuarioRepository;
        this.passwordVerifier = passwordVerifier;
    }

    @Override
    public String autenticar(String username, String senha) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (!passwordVerifier.verificar(senha, usuario.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        return tokenGenerator.gerarToken(usuario.getUsername());
    }
}
