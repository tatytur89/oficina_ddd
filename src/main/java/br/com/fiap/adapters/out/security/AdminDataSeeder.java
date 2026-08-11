package br.com.fiap.adapters.out.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.fiap.domain.entities.Usuario;
import br.com.fiap.ports.out.UsuarioRepositoryPort;

@Component
public class AdminDataSeeder implements CommandLineRunner {

  private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-user:admin}")
    private String defaultUser;

    @Value("${app.admin.default-password:admin123}")
    private String defaultPassword;

    public AdminDataSeeder(UsuarioRepositoryPort usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.buscarPorUsername(defaultUser).isEmpty()) {
            String senhaCriptografada = passwordEncoder.encode(defaultPassword);
            
            Usuario admin = new Usuario(null, defaultUser, senhaCriptografada, "ROLE_ADMIN");
            usuarioRepository.salvar(admin);
            
            System.out.println("====== Usuário administrador gerado no banco com sucesso ======");
        }
    }
    

}
